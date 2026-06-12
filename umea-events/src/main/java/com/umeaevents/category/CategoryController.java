package com.umeaevents.category;

import com.umeaevents.category.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-lagret för kategorier. Tunt med flit: tar emot HTTP, delegerar till
 * service:n, returnerar DTO:er. @Tag/@Operation dyker upp i Swagger UI.
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Eventkategorier")
public class CategoryController {

    private final CategoryService service;

    @GetMapping
    @Operation(summary = "Lista alla aktiva kategorier")
    public ResponseEntity<List<CategoryResponse>> getAll() {
        return ResponseEntity.ok(service.findAllActive());
    }
}
