package com.umeaevents.category.dto;

import java.util.UUID;

/**
 * Utgående representation av en kategori. En record passar perfekt för
 * DTO:er: immutabel, kortfattad, ingen Lombok behövs. Detta är API-kontraktet
 * — entiteten Category exponeras aldrig direkt.
 */
public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active
) {
}
