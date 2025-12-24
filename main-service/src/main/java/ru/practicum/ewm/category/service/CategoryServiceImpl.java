package ru.practicum.ewm.category.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.dto.CategoryDto;
import ru.practicum.ewm.category.dto.NewCategoryDto;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.repository.CategoryRepository;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;
    private final EntityManager entityManager;


    // ============================================================
    // Создание категории
    // ============================================================
    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto dto) {
        Category category = mapper.toCategory(dto);
        try {
            Category saved = repository.save(category);
            return mapper.toCategoryDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category name already exists: " + dto.getName());
        }
    }


    // ============================================================
    // Обновление категории
    // ============================================================
    @Override
    @Transactional
    public CategoryDto updateCategory(Long id, NewCategoryDto dto) {
        Category category = repository.findById(id)
                                      .orElseThrow(() ->
                                              new NotFoundException("Category with id=" + id + " was not found"));

        category.setName(dto.getName());
        try {
            Category saved = repository.saveAndFlush(category);
            return mapper.toCategoryDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Category name already exists: " + dto.getName());
        }
    }


    // ============================================================
    // Удаление категории
    // ============================================================
    @Override
    @Transactional
    public void deleteCategory(Long id) {

        if (!repository.existsById(id)) {
            throw new NotFoundException("Category with id=" + id + " was not found");
        }

        // Проверка наличия привязанных событий
        boolean hasEvents = eventRepository.existsByCategoryId(id);
        if (hasEvents) {
            throw new ConflictException("Cannot delete category with attached events");
        }

        repository.deleteById(id);
    }


    // ============================================================
    // Получение списка категорий с пагинацией
    // ============================================================
    @Override
    public List<CategoryDto> findCategoriesWithPagination(int from, int size) {

        return repository.findCategoriesWithPagination(from, size, entityManager)
                         .stream()
                         .map(mapper::toCategoryDto)
                         .toList();
    }


    // ============================================================
    // Получение категории по id
    // ============================================================
    @Override
    public CategoryDto getCategoryById(Long id) {
        Category category = repository.findById(id)
                                      .orElseThrow(() ->
                                              new NotFoundException("Category with id=" + id + " was not found"));
        return mapper.toCategoryDto(category);
    }
}
