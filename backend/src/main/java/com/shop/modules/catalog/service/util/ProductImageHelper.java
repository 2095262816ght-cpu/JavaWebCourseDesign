package com.shop.modules.catalog.service.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品图片序列化与画廊解析公共工具
 */
public final class ProductImageHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ProductImageHelper() {
    }

    /**
     * 将副图列表格式化为逗号分隔的持久化字符串
     */
    public static String formatSubImages(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    /**
     * 解析副图字段（兼容 JSON 数组与逗号分隔字符串）
     */
    public static List<String> parseSubImages(String rawSubImages) {
        if (!StringUtils.hasText(rawSubImages)) {
            return Collections.emptyList();
        }
        if (rawSubImages.startsWith("[") && rawSubImages.endsWith("]")) {
            try {
                List<String> parsed = OBJECT_MAPPER.readValue(rawSubImages, new TypeReference<List<String>>() {});
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return Arrays.stream(rawSubImages.split(","))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
    }

    /**
     * 解析商品详情画廊图片（若副图为空，则以主图保底供画廊展示）
     */
    public static List<String> parseGalleryImages(String mainImage, String rawSubImages) {
        List<String> images = new ArrayList<>(parseSubImages(rawSubImages));
        if (images.isEmpty() && StringUtils.hasText(mainImage)) {
            images.add(mainImage);
        }
        return images;
    }
}
