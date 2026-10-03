package com.charles.calendrierfoyer.service;

import com.charles.calendrierfoyer.domain.Category;
import com.charles.calendrierfoyer.dto.CategoryCreateDto;
import com.charles.calendrierfoyer.dto.CategoryDto;
import com.charles.calendrierfoyer.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private static final String NAME_TAKEN = "Une categorie porte deja ce nom.";

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryDto> findAll() {
        return categoryRepository.findAllByOrderByPositionAscIdAsc().stream()
                .map(CategoryService::toDto)
                .toList();
    }

    @Transactional
    public CategoryDto create(CategoryCreateDto dto) {
        String name = dto.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(NAME_TAKEN);
        }
        Category c = new Category();
        c.setName(name);
        c.setIcon(dto.icon().trim());
        c.setPosition(categoryRepository.maxPosition() + 1);
        return toDto(categoryRepository.save(c));
    }

    @Transactional
    public CategoryDto update(Long id, CategoryCreateDto dto) {
        Category c = getOrThrow(id);
        String name = dto.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(NAME_TAKEN);
        }
        c.setName(name);
        c.setIcon(dto.icon().trim());
        return toDto(categoryRepository.save(c));
    }

    /** Les evenements de cette categorie sont conserves, sans categorie (ON DELETE SET NULL). */
    @Transactional
    public void delete(Long id) {
        categoryRepository.delete(getOrThrow(id));
    }

    Category getOrThrow(Long id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Categorie introuvable: " + id));
    }

    private static CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getIcon());
    }
}
