package com.charles.calendrierfoyer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.charles.calendrierfoyer.domain.Category;
import com.charles.calendrierfoyer.dto.CategoryCreateDto;
import com.charles.calendrierfoyer.dto.CategoryDto;
import com.charles.calendrierfoyer.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static Category category(Long id, String name, String icon, int position) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setIcon(icon);
        c.setPosition(position);
        return c;
    }

    @Test
    void lesCategoriesSontRenvoyeesDansLOrdre() {
        when(categoryRepository.findAllByOrderByPositionAscIdAsc())
                .thenReturn(List.of(category(1L, "Anniversaire", "🎂", 1), category(2L, "Jardin", "🌻", 2)));

        assertThat(categoryService.findAll()).extracting(CategoryDto::icon).containsExactly("🎂", "🌻");
    }

    @Test
    void creerUneCategorieLaPlaceALaFin() {
        when(categoryRepository.existsByNameIgnoreCase("Piscine")).thenReturn(false);
        when(categoryRepository.maxPosition()).thenReturn(22);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryDto dto = categoryService.create(new CategoryCreateDto(" Piscine ", "🏊"));

        assertThat(dto.name()).isEqualTo("Piscine");
        assertThat(dto.icon()).isEqualTo("🏊");
        verify(categoryRepository).save(org.mockito.ArgumentMatchers.argThat(c -> c.getPosition() == 23));
    }

    @Test
    void deuxCategoriesNePeuventPasAvoirLeMemeNom() {
        when(categoryRepository.existsByNameIgnoreCase("Jardin")).thenReturn(true);
        CategoryCreateDto dto = new CategoryCreateDto("Jardin", "🌱");

        assertThatThrownBy(() -> categoryService.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void modifierUneCategorie() {
        Category c = category(1L, "Jardin", "🌻", 1);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Potager", 1L)).thenReturn(false);
        when(categoryRepository.save(c)).thenReturn(c);

        CategoryDto dto = categoryService.update(1L, new CategoryCreateDto("Potager", "🥕"));

        assertThat(dto.name()).isEqualTo("Potager");
        assertThat(dto.icon()).isEqualTo("🥕");
    }

    @Test
    void renommerAvecLeNomDUneAutreEstRefuse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Jardin", "🌻", 1)));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Sport", 1L)).thenReturn(true);
        CategoryCreateDto dto = new CategoryCreateDto("Sport", "🌻");

        assertThatThrownBy(() -> categoryService.update(1L, dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void supprimerUneCategorie() {
        Category c = category(1L, "Jardin", "🌻", 1);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(c));

        categoryService.delete(1L);

        verify(categoryRepository).delete(c);
    }

    @Test
    void supprimerUneCategorieInconnueLeveUneErreur() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(1L)).isInstanceOf(EntityNotFoundException.class);
    }
}
