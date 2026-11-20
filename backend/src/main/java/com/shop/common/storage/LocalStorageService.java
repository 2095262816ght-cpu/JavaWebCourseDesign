package com.shop.common.storage;

import com.shop.common.exception.BusinessException;
import com.shop.common.result.ResultCode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".ico"
    );

    private final StorageProperties properties;
    private Path rootLocation;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            log.info("本地存储目录初始化完成: {}", this.rootLocation);
        } catch (IOException e) {
            throw new BusinessException("无法初始化本地文件存储目录");
        }
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf(".");
        if (dotIdx >= 0) {
            extension = originalFilename.substring(dotIdx).toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)
                || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("svg"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的文件类型，仅支持 JPG/PNG/GIF/WebP/BMP/ICO 图片格式");
        }

        String storedFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        try {
            if (storedFilename.contains("..")) {
                throw new BusinessException("非法的文件名路径");
            }

            Path destinationFile = this.rootLocation.resolve(storedFilename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                throw new BusinessException("禁止保存至存储根目录之外");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("文件保存成功: {} -> {}", originalFilename, storedFilename);
            String prefix = properties.getUrlPrefix();
            if (!prefix.endsWith("/")) {
                prefix += "/";
            }
            return prefix + storedFilename;
        } catch (IOException e) {
            log.error("保存上传文件失败", e);
            throw new BusinessException("文件存储写入失败");
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException("请求的文件不存在或无法读取: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException("读取文件失败: " + filename);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", filename, e);
        }
    }
}
