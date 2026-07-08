package com.umeaevents.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Uploads image bytes to a public Supabase Storage bucket via its REST API and returns the
 * public URL. Uses Spring's {@link RestClient} (no extra SDK dependency). The service key stays
 * server-side (Fly secret); the resulting URL is public because the bucket is public.
 */
@Service
public class SupabaseStorageService {

    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final RestClient restClient = RestClient.create();

    public SupabaseStorageService(
            @Value("${app.storage.supabase-url:}") String supabaseUrl,
            @Value("${app.storage.service-key:}") String serviceKey,
            @Value("${app.storage.bucket:event-images}") String bucket) {
        this.supabaseUrl = stripTrailingSlash(supabaseUrl);
        this.serviceKey = serviceKey;
        this.bucket = bucket;
    }

    /** Store the bytes under a random object key; returns the public URL to the stored object. */
    public String upload(byte[] content, String contentType, String extension) {
        if (supabaseUrl.isBlank() || serviceKey.isBlank()) {
            throw new ImageUploadException("Bildlagring är inte konfigurerad");
        }
        String objectKey = "uploads/" + UUID.randomUUID() + (extension == null ? "" : "." + extension);
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectKey;
        try {
            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ImageUploadException("Kunde inte ladda upp bilden till lagringen", e);
        }
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectKey;
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
