package com.shopper.availability;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockController.class)
@Import(StockData.class)
class StockControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void returnsStockForMarket() throws Exception {
        mvc.perform(get("/stock/P-1001").param("market", "pl-PL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(42))
                .andExpect(jsonPath("$.warehouse").value("Poznań"));
    }

    @Test
    void returns404ForUnknownProduct() throws Exception {
        mvc.perform(get("/stock/P-9999").param("market", "pl-PL"))
                .andExpect(status().isNotFound());
    }
}
