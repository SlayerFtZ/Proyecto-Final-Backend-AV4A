package com.inventaria.api.categories.domain;

import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.categories.presentation.CategoryRequest;
import com.inventaria.api.categories.presentation.CategoryResponse;
import com.inventaria.api.shared.error.DuplicateNameException;
import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryJpaRepository repository;

    public CategoryService(CategoryJpaRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> findAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse findById(Integer id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateNameException("categoría", request.name());
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));

        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new DuplicateNameException("categoría", request.name());
        }

        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Categoría", id);
        }
        if (repository.countProductsByCategoryId(id) > 0) {
            throw new RecordInUseException("categoría", id);
        }
        repository.deleteById(id);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
