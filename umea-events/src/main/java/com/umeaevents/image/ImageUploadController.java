package com.umeaevents.image;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Image upload for event/venue pictures. Staff (RESTAURANT/ADMIN) upload a file; the stored
 * public URL is returned and dropped into the usual {@code imageUrl} fields. Size is capped by
 * {@code spring.servlet.multipart.max-file-size}; type is restricted to common image formats.
 */
@RestController
@RequestMapping("/api/v1/images")
@PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Bilduppladdning")
public class ImageUploadController {

    private static final Map<String, String> ALLOWED_TYPE_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif");

    private final SupabaseStorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ladda upp en bild och få tillbaka en publik URL",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ImageUploadResponse upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Ingen fil bifogad");
        }
        String type = file.getContentType();
        String extension = type == null ? null : ALLOWED_TYPE_TO_EXT.get(type.toLowerCase());
        if (extension == null) {
            throw new IllegalArgumentException("Endast bildfiler tillåts (jpg, png, webp, gif)");
        }
        try {
            String url = storage.upload(file.getBytes(), type, extension);
            return new ImageUploadResponse(url);
        } catch (IOException e) {
            throw new ImageUploadException("Kunde inte läsa den uppladdade filen", e);
        }
    }
}
