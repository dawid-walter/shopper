package com.shopper.pricing.presentation.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PricingApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void unpricedProductIs404() throws Exception {
        mvc.perform(get("/prices/P-9999").param("market", "pl-PL")).andExpect(status().isNotFound());
    }

    @Test
    void everyMarketIsPricedInItsCurrencyAndVat() throws Exception {
        mvc.perform(get("/prices/P-1001").param("market", "nl-NL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.vat").value(5.46))
                .andExpect(jsonPath("$.finalPrice").value(31.46));
    }

    @Test
    void vatRateAddedForNextYearShowsOnlyFromThen() throws Exception {
        mvc.perform(post("/components/vat-pl/versions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"rate": 24, "validFrom": "2027-01-01T00:00:00Z", "markets": ["pl-PL"]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versions").value(2));

        mvc.perform(get("/prices/P-1001").param("market", "pl-PL").param("at", "2026-12-31T12:00:00Z"))
                .andExpect(jsonPath("$.vat").value(23.00));
        mvc.perform(get("/prices/P-1001").param("market", "pl-PL").param("at", "2027-01-02T12:00:00Z"))
                .andExpect(jsonPath("$.vat").value(24.00))
                .andExpect(jsonPath("$.breakdown.length()").value(2));
    }
}
