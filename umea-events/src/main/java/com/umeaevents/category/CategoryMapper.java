package com.umeaevents.category;

import com.umeaevents.category.dto.CategoryResponse;
import org.springframework.stereotype.Component;

/**
 * Översätter entitet -> DTO. Medvetet en handskriven mapper i denna milestone:
 * noll byggkonfiguration, inga annotation-processorer, lätt att läsa.
 *
 * När antalet entiteter växer (M4 och framåt) byter vi till MapStruct, som
 * genererar mappar-koden och tar bort boilerplate. Just nu vore det
 * överteknik.
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.isActive()
        );
    }
}
