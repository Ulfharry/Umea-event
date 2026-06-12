package com.umeaevents.category;

import com.umeaevents.category.dto.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest startar bara webblagret (ingen databas, ingen full kontext) och
 * mockar service:n. Snabbt och isolerat — verifierar routing, statuskod och
 * JSON-form för GET /api/v1/categories.
 */
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getAll_returnsCategories() throws Exception {
        var pubquiz = new CategoryResponse(
                UUID.randomUUID(), "Pubquiz", "pubquiz", "Frågesport på pub", true);
        when(categoryService.findAllActive()).thenReturn(List.of(pubquiz));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pubquiz"))
                .andExpect(jsonPath("$[0].slug").value("pubquiz"));
    }
}
