package com.umeaevents.image;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
class ImageUploadControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private SupabaseStorageService storage;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void upload_image_returnsPublicUrl() throws Exception {
        when(storage.upload(any(), eq("image/png"), eq("png")))
                .thenReturn("https://x.supabase.co/storage/v1/object/public/event-images/uploads/a.png");
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url")
                        .value("https://x.supabase.co/storage/v1/object/public/event-images/uploads/a.png"));
    }

    @Test
    @WithMockUser(roles = "RESTAURANT")
    void upload_nonImage_returns400() throws Exception {
        var file = new MockMultipartFile("file", "notes.txt", "text/plain", "hej".getBytes());

        mockMvc.perform(multipart("/api/v1/images").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void upload_asPlainUser_returns403() throws Exception {
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/images").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_withoutAuth_returns401() throws Exception {
        var file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/images").file(file))
                .andExpect(status().isUnauthorized());
    }
}
