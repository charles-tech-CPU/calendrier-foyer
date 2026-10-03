package com.charles.calendrierfoyer.web;

import com.charles.calendrierfoyer.dto.CategoryCreateDto;
import com.charles.calendrierfoyer.dto.CategoryDto;
import com.charles.calendrierfoyer.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryDto> findAll() {
        return categoryService.findAll();
    }

    @PostMapping
    public CategoryDto create(@Valid @RequestBody CategoryCreateDto dto) {
        return categoryService.create(dto);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable Long id, @Valid @RequestBody CategoryCreateDto dto) {
        return categoryService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
