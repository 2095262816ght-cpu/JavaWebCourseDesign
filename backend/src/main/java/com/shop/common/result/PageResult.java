package com.shop.common.result;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "通用分页结果封装")
public record PageResult<T>(
        @Schema(description = "当前页码", example = "1")
        long current,

        @Schema(description = "每页记录数", example = "10")
        long size,

        @Schema(description = "总记录数", example = "42")
        long total,

        @Schema(description = "总页数", example = "5")
        long pages,

        @Schema(description = "数据记录列表")
        List<T> records
) {
    public static <T> PageResult<T> of(long current, long size, long total, List<T> records) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(current, size, total, pages, records != null ? records : List.of());
    }
}
