package com.shopper.customer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@Import(CustomerData.class)
class CustomerControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void returnsCustomerSegment() throws Exception {
        mvc.perform(get("/customers/C-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segment").value("DEALER"));
    }

    @Test
    void returns404ForUnknownCustomer() throws Exception {
        mvc.perform(get("/customers/C-404"))
                .andExpect(status().isNotFound());
    }
}
