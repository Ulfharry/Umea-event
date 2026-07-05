package com.umeaevents.category;

import com.umeaevents.category.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Admin management of categories (currently: the stock image used as event fallback). */
@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin categories")
public class AdminCategoryController {

    private final CategoryService service;

    @PatchMapping("/{id}")
    @Operation(summary = "Sätt stockbild för en kategori", security = @SecurityRequirement(name = "bearerAuth"))
    public CategoryResponse updateImage(@PathVariable UUID id, @RequestBody UpdateCategoryImageRequest request) {
        return service.updateImage(id, request.imageUrl());
    }

    public record UpdateCategoryImageRequest(String imageUrl) {}
}
