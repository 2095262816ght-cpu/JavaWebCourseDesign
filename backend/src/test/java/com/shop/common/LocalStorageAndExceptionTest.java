package com.shop.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.GlobalExceptionHandler;
import com.shop.common.result.Result;
import com.shop.common.storage.LocalStorageService;
import com.shop.common.storage.StorageProperties;
import com.shop.modules.auth.dto.LoginCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MissingServletRequestParameterException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LocalStorageAndExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Value("${shop.storage.local-dir:./uploads}")
    private String configuredUploadDir;

    private String adminToken;

    @BeforeEach
    void loginAdmin() throws Exception {
        String responseStr = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginCommand("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(responseStr).path("data").path("tokenValue").asText();
    }

    @Test
    @DisplayName("单元测试 LocalStorageService 真实文件读写与 SVG/非法扩展名安全拦截")
    void testLocalStorageServiceStoreAndSecurityGuards(@TempDir Path tempDir) throws Exception {
        StorageProperties props = new StorageProperties();
        props.setLocalDir(tempDir.toString());
        props.setUrlPrefix("/uploads");
        LocalStorageService storageService = new LocalStorageService(props);
        storageService.init();

        byte[] pngPayload = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile validPng = new MockMultipartFile("file", "sample.PNG", "image/png", pngPayload);
        String url = storageService.store(validPng);

        assertThat(url).startsWith("/uploads/").endsWith(".png");
        String storedName = url.substring("/uploads/".length());
        Path storedPath = tempDir.resolve(storedName);
        assertThat(Files.exists(storedPath)).isTrue();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(pngPayload);

        // 拒绝 SVG 扩展名，防止 Stored XSS
        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "xss.svg",
                "image/svg+xml",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> storageService.store(svgFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");

        // 拒绝伪装为 .png 但 Content-Type 为 image/svg+xml 的文件
        MockMultipartFile spoofedSvg = new MockMultipartFile(
                "file",
                "spoofed.png",
                "image/svg+xml",
                "<svg/>".getBytes(StandardCharsets.UTF_8)
        );
        assertThatThrownBy(() -> storageService.store(spoofedSvg))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");

        // 拒绝不支持的扩展名与空文件
        MockMultipartFile txtFile = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> storageService.store(txtFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型");

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    @DisplayName("集成测试 /api/common/upload 运行时落盘与 SVG 拒绝")
    void testCommonUploadEndpointRuntimeAndCleanup() throws Exception {
        byte[] content = "runtime-png-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile validFile = new MockMultipartFile("file", "logo.png", "image/png", content);

        String responseStr = mockMvc.perform(multipart("/api/common/upload")
                        .file(validFile)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data").isString())
                .andReturn().getResponse().getContentAsString();

        String returnedUrl = objectMapper.readTree(responseStr).path("data").asText();
        assertThat(returnedUrl).startsWith("/uploads/").endsWith(".png");

        String filename = returnedUrl.substring("/uploads/".length());
        Path diskFile = Paths.get(configuredUploadDir).toAbsolutePath().normalize().resolve(filename);
        try {
            assertThat(Files.exists(diskFile)).isTrue();
            assertThat(Files.readAllBytes(diskFile)).isEqualTo(content);
        } finally {
            Files.deleteIfExists(diskFile);
        }

        // SVG 上传被拒绝并返回 400
        MockMultipartFile svgFile = new MockMultipartFile(
                "file",
                "bad.svg",
                "image/svg+xml",
                "<svg><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/common/upload")
                        .file(svgFile)
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("不支持的文件类型")));
    }

    @Test
    @DisplayName("集成测试 GlobalExceptionHandler 处理 400/404/MethodNotSupported 异常映射")
    void testGlobalExceptionHandlerMappings() throws Exception {
        // 1. BindException (@Valid 校验失败 -> 400)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));

        // 2. HttpMessageNotReadableException (畸形 JSON -> 400)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));

        // 3. MethodArgumentTypeMismatchException (路径参数类型不匹配 -> 400)
        mockMvc.perform(get("/api/portal/products/not-a-number"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));

        // 4. MissingServletRequestPartException & HttpMediaTypeNotSupportedException (400)
        mockMvc.perform(multipart("/api/common/upload")
                        .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));

        mockMvc.perform(post("/api/common/upload")
                        .header("satoken", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)));

        // 5. MissingServletRequestParameterException (直接校验 handler 映射 400)
        Result<Void> missingParamRes = globalExceptionHandler.handleBadRequestException(
                new MissingServletRequestParameterException("limit", "int")
        );
        assertThat(missingParamRes.code()).isEqualTo(400);

        // 6. HttpRequestMethodNotSupportedException (方法不允许 -> 400 提示不支持的请求方法)
        mockMvc.perform(delete("/api/portal/brands/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", containsString("不支持的请求方法")));

        // 7. NoResourceFoundException (未知接口路径 -> 404)
        mockMvc.perform(get("/api/nonexistent/resource-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(404)));
    }
}
