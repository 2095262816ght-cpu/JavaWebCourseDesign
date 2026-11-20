package com.shop.modules.catalog.controller;

import com.shop.common.result.Result;
import com.shop.modules.catalog.dto.CategoryCommand;
import com.shop.modules.catalog.dto.CategoryNodeVO;
import com.shop.modules.catalog.service.CategoryService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理后台-商品分类管理", description = "系统管理员对商品分类的多级树状维护与增删改")
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "查询全部分类树", description = "管理员后台树形表格展示分类结构（包含已禁用分类）")
    @GetMapping({"", "/tree"})
    public Result<List<CategoryNodeVO>> getCategoryTree() {
        List<CategoryNodeVO> tree = categoryService.getAdminCategoryTree();
        return Result.success(tree);
    }

    @Operation(summary = "创建新分类", description = "支持创建根分类或指定父分类的二级/三级子分类")
    @PostMapping
    public Result<Void> createCategory(@Valid @RequestBody CategoryCommand cmd) {
        categoryService.saveCategory(cmd);
        return Result.success("分类创建成功", null);
    }

    @Operation(summary = "修改分类信息", description = "更新分类名称、图标、父级节点、排序序号或启用状态")
    @PutMapping("/{id}")
    public Result<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryCommand cmd
    ) {
        CategoryCommand commandWithId = new CategoryCommand(
                id,
                cmd.parentId(),
                cmd.name(),
                cmd.icon(),
                cmd.sort(),
                cmd.status()
        );
        categoryService.saveCategory(commandWithId);
        return Result.success("分类更新成功", null);
    }

    @Operation(summary = "删除指定分类", description = "若分类下存在子分类则安全拦截不允许删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success("分类删除成功", null);
    }
}
