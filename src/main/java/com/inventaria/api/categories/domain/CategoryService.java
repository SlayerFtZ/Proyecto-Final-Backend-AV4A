package com.inventaria.api.categories.domain;

import com.inventaria.api.categories.infrastructure.CategoryJpaRepository;
import com.inventaria.api.categories.presentation.CategoryRequest;
import com.inventaria.api.categories.presentation.CategoryResponse;
import com.inventaria.api.shared.error.DuplicateNameException;
import com.inventaria.api.shared.error.RecordInUseException;
import com.inventaria.api.shared.error.ResourceNotFoundException;
import com.inventaria.api.shared.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryJpaRepository repository;
    private final CurrentUser currentUser;

    public CategoryService(CategoryJpaRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public List<CategoryResponse> findAll() {
        return repository.findAllByUserId(currentUser.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse findById(Integer id) {
        return toResponse(getOwned(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Integer userId = currentUser.getId();
        if (repository.existsByUserIdAndNameIgnoreCase(userId, request.name())) {
            throw new DuplicateNameException("categoría", request.name());
        }
        Category category = new Category();
        category.setUserId(userId);
        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest request) {
        Category category = getOwned(id);

        if (repository.existsByUserIdAndNameIgnoreCaseAndIdNot(category.getUserId(), request.name(), id)) {
            throw new DuplicateNameException("categoría", request.name());
        }

        category.setName(request.name());
        category.setDescription(request.description());
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Integer id) {
        if (!repository.existsByIdAndUserId(id, currentUser.getId())) {
            throw new ResourceNotFoundException("Categoría", id);
        }
        if (repository.countProductsByCategoryId(id) > 0) {
            throw new RecordInUseException("categoría", id);
        }
        repository.deleteById(id);
    }

    /** Devuelve la categoría solo si pertenece al usuario actual; si no, 404. */
    private Category getOwned(Integer id) {
        return repository.findByIdAndUserId(id, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }
}
