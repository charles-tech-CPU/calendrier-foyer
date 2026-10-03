package com.charles.calendrierfoyer.web;

import static org.mockito.Mockito.verify;

import com.charles.calendrierfoyer.dto.CategoryCreateDto;
import com.charles.calendrierfoyer.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    @Test
    void chaqueRouteDelegueAuService() {
        CategoryCreateDto dto = new CategoryCreateDto("Jardin", "🌻");

        controller.findAll();
        controller.create(dto);
        controller.update(1L, dto);
        controller.delete(1L);

        verify(categoryService).findAll();
        verify(categoryService).create(dto);
        verify(categoryService).update(1L, dto);
        verify(categoryService).delete(1L);
    }
}
