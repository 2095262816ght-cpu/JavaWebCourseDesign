package com.shop.common.controller;

import com.shop.common.result.Result;
import com.shop.common.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "通用公共接口", description = "文件上传与通用能力")
@RestController
@RequestMapping("/api/common")
public class CommonUploadController {

    private final StorageService storageService;

    public CommonUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @Operation(summary = "通用单文件上传", description = "支持图片、Logo等静态资源上传，返回公开访问URL")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String url = storageService.store(file);
        return Result.success("上传成功", url);
    }
}
