package com.umeaevents.category;

import com.umeaevents.category.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
