package ru.practicum.ewm.category.service;

import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto dto);

    CategoryDto updateCategory(Long id, NewCategoryDto dto);

    void deleteCategory(Long id);

    List<CategoryDto> findCategoriesWithPagination(int from, int size);

    CategoryDto getCategoryById(Long id);
}
