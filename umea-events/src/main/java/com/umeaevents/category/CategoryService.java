package com.umeaevents.category;

import com.umeaevents.category.dto.CategoryResponse;
import com.umeaevents.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Affärslogiklagret. Controllern hanterar HTTP, repository hanterar SQL —
 * service:n hanterar logiken däremellan och äger transaktionsgränsen.
 *
 * @RequiredArgsConstructor (Lombok) genererar konstruktorn för de final-fält
 * Spring injicerar. Konstruktorinjektion > fältinjektion: testbart och gör
 * beroenden explicita.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAllActive() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** Admin: set the stock image URL for a category. */
    @Transactional
    public CategoryResponse updateImage(UUID id, String imageUrl) {
        Category category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategori hittades inte: " + id));
        category.setImageUrl(imageUrl);
        return mapper.toResponse(repository.save(category));
    }
}
