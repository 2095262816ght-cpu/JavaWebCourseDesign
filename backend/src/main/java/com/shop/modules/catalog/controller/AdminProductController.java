package com.shop.modules.catalog.controller;

import com.shop.common.exception.BusinessException;
import com.shop.common.result.PageResult;
import com.shop.common.result.Result;
import com.shop.common.result.ResultCode;
import com.shop.modules.catalog.dto.AdminProductQuery;
import com.shop.modules.catalog.dto.ProductAdminVO;
import com.shop.modules.catalog.dto.ProductCommand;
import com.shop.modules.catalog.dto.ProductStatusUpdateDTO;
import com.shop.modules.catalog.service.ProductAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-商品管理", description = "系统管理员对商品信息的全量管理：新建、编辑、上下架切换、删除及复合检索")
@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductAdminService productAdminService;

    public AdminProductController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @Operation(summary = "多条件分页查询商品列表", description = "支持按商品标题、所属品牌、所属分类以及上下架状态检索")
    @GetMapping
    public Result<PageResult<ProductAdminVO>> listAdminProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        AdminProductQuery query = new AdminProductQuery(name, brandId, categoryId, status, pageNum, pageSize);
        PageResult<ProductAdminVO> pageResult = productAdminService.listAdminProducts(query);
        return Result.success(pageResult);
    }

    @Operation(summary = "根据商品ID获取后台商品详情")
    @GetMapping("/{id}")
    public Result<ProductAdminVO> getAdminProduct(@PathVariable Long id) {
        ProductAdminVO vo = productAdminService.getAdminProductById(id);
        return Result.success(vo);
    }

    @Operation(summary = "新建商品")
    @PostMapping
    public Result<Long> createProduct(@Valid @RequestBody ProductCommand cmd) {
        Long productId = productAdminService.saveProduct(cmd);
        return Result.success("商品创建成功", productId);
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public Result<Void> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductCommand cmd
    ) {
        ProductCommand commandWithId = new ProductCommand(
                id,
                cmd.name(),
                cmd.subtitle(),
                cmd.brandId(),
                cmd.categoryId(),
                cmd.mainImage(),
                cmd.subImages(),
                cmd.price(),
                cmd.originalPrice(),
                cmd.stock(),
                cmd.status(),
                cmd.detailHtml()
        );
        productAdminService.saveProduct(commandWithId);
        return Result.success("商品更新成功", null);
    }

    @Operation(summary = "快速切换商品上下架状态")
    @PatchMapping("/{id}/status")
    public Result<Void> updateProductStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status,
            @RequestBody(required = false) ProductStatusUpdateDTO statusDto
    ) {
        Integer targetStatus = status != null ? status : (statusDto != null ? statusDto.status() : null);
        if (targetStatus == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "上下架状态值不能为空");
        }
        productAdminService.updateStatus(id, targetStatus);
        return Result.success("商品上下架状态已更新", null);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productAdminService.deleteProduct(id);
        return Result.success("商品已删除", null);
    }
}
