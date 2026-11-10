package com.shop.modules.catalog.controller;

import com.shop.common.result.PageResult;
import com.shop.common.result.Result;
import com.shop.modules.catalog.dto.BrandCommand;
import com.shop.modules.catalog.dto.BrandQuery;
import com.shop.modules.catalog.dto.BrandVO;
import com.shop.modules.catalog.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-商品品牌管理", description = "系统管理员对商品品牌的增删改查与检索控制")
@RestController
@RequestMapping("/api/admin/brands")
public class AdminBrandController {

    private final BrandService brandService;

    public AdminBrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @Operation(summary = "分页查询品牌列表", description = "支持按品牌名/描述模糊搜索以及状态筛选")
    @GetMapping
    public Result<PageResult<BrandVO>> listBrands(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        BrandQuery query = new BrandQuery(page, size, keyword, status);
        PageResult<BrandVO> result = brandService.listBrands(query);
        return Result.success(result);
    }

    @Operation(summary = "查询指定品牌详情")
    @GetMapping("/{id}")
    public Result<BrandVO> getBrand(@PathVariable Long id) {
        BrandVO brand = brandService.getBrandById(id);
        return Result.success(brand);
    }

    @Operation(summary = "新增商品品牌")
    @PostMapping
    public Result<Void> createBrand(@Valid @RequestBody BrandCommand cmd) {
        brandService.saveBrand(cmd);
        return Result.success("品牌创建成功", null);
    }

    @Operation(summary = "更新商品品牌")
    @PutMapping("/{id}")
    public Result<Void> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandCommand cmd
    ) {
        BrandCommand commandWithId = new BrandCommand(
                id,
                cmd.name(),
                cmd.origin(),
                cmd.logo(),
                cmd.description(),
                cmd.sort(),
                cmd.status()
        );
        brandService.saveBrand(commandWithId);
        return Result.success("品牌更新成功", null);
    }

    @Operation(summary = "删除指定商品品牌")
    @DeleteMapping("/{id}")
    public Result<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return Result.success("品牌删除成功", null);
    }
}
