package com.shopper.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
@Import(CatalogData.class)
class CatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void returnsProductInRequestedLanguage() throws Exception {
        mvc.perform(get("/products/P-1001").param("locale", "pl-PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Filtr oleju"))
                .andExpect(jsonPath("$.language").value("pl"));
    }

    @Test
    void fallsBackToEnglishForUnknownLanguage() throws Exception {
        mvc.perform(get("/products/P-1001").param("locale", "fr-FR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Oil filter"));
    }

    @Test
    void returns404ForUnknownProduct() throws Exception {
        mvc.perform(get("/products/P-9999").param("locale", "pl-PL"))
                .andExpect(status().isNotFound());
    }
}
