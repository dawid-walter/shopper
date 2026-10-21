package com.shopper.aggregator.component;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest(properties = "aggregator.circuit-breaker.window=4")
@AutoConfigureMockMvc
class ProductEndpointComponentTest {

    @RegisterExtension
    static WireMockExtension upstreams = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void upstreamUrls(DynamicPropertyRegistry registry) {
        for (String name : new String[] {"catalog", "pricing", "availability", "customer"}) {
            registry.add("aggregator.upstreams." + name + ".url", upstreams::baseUrl);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    CircuitBreakerRegistry circuitBreakers;

    @Autowired
    MeterRegistry meters;

    @BeforeEach
    void allUpstreamsHealthy() {
        circuitBreakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        stubCatalog(okJson("""
                {"name": "Filtr oleju", "description": "Filtr oleju do silników ciągników.",
                 "specs": {"thread": "M20x1.5"}, "images": []}"""));
        stubPricing(okJson("""
                {"currency": "PLN", "basePrice": 100.00, "discount": -10.00, "netPrice": 90.00,
                 "vat": 20.70, "finalPrice": 110.70, "breakdown": []}"""));
        stubStock(okJson("""
                {"quantity": 42, "warehouse": "Poznań", "deliveryInDays": 1}"""));
        upstreams.stubFor(get(urlPathEqualTo("/customers/C-42")).willReturn(okJson("""
                {"segment": "DEALER", "preferences": ["fast-delivery"]}""")));
    }

    private void stubCatalog(ResponseDefinitionBuilder response) {
        upstreams.stubFor(get(urlPathEqualTo("/products/P-1001")).willReturn(response));
    }

    private void stubPricing(ResponseDefinitionBuilder response) {
        upstreams.stubFor(get(urlPathEqualTo("/prices/P-1001")).willReturn(response));
    }

    private void stubStock(ResponseDefinitionBuilder response) {
        upstreams.stubFor(get(urlPathEqualTo("/stock/P-1001")).willReturn(response));
    }

    private ResultActions request(String market) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get("/products/P-1001")
                .param("market", market)
                .param("customerId", "C-42"));
    }

    @Test
    void allUpstreamsAnswer() throws Exception {
        request("pl-PL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.product.name").value("Filtr oleju"))
                .andExpect(jsonPath("$.price.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.price.finalPrice").value(110.70))
                .andExpect(jsonPath("$.stock.status").value("KNOWN"))
                .andExpect(jsonPath("$.customer.status").value("PERSONALIZED"));
    }

    @Test
    void marketIsPassedToEveryUpstream() throws Exception {
        request("pl-PL").andExpect(status().isOk());

        upstreams.verify(getRequestedFor(urlPathEqualTo("/products/P-1001"))
                .withQueryParam("locale", equalTo("pl-PL")));
        upstreams.verify(getRequestedFor(urlPathEqualTo("/prices/P-1001")).withQueryParam("market", equalTo("pl-PL"))
                .withQueryParam("customerId", equalTo("C-42")));
        upstreams.verify(getRequestedFor(urlPathEqualTo("/stock/P-1001")).withQueryParam("market", equalTo("pl-PL")));
    }

    @Test
    void traceContextReachesEveryUpstream() throws Exception {
        String traceId = request("pl-PL").andReturn().getResponse().getHeader("X-Trace-Id");

        for (String path : new String[] {"/products/P-1001", "/prices/P-1001", "/stock/P-1001", "/customers/C-42"}) {
            upstreams.verify(getRequestedFor(urlPathEqualTo(path)).withHeader("traceparent", containing(traceId)));
        }
    }

    @Test
    void unknownProductIs404() throws Exception {
        stubCatalog(aResponse().withStatus(404));

        request("pl-PL").andExpect(status().isNotFound());
    }

    @Test
    void failingCatalogIs503() throws Exception {
        stubCatalog(aResponse().withStatus(500));

        request("pl-PL").andExpect(status().isServiceUnavailable());
    }

    @Test
    void failingPricingDegradesOnlyThePrice() throws Exception {
        stubPricing(aResponse().withStatus(500));

        request("pl-PL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.price.finalPrice").doesNotExist())
                .andExpect(jsonPath("$.stock.status").value("KNOWN"));
    }

    @Test
    void hangingPricingDoesNotHoldTheResponse() throws Exception {
        stubPricing(okJson("{}").withFixedDelay(2_000));

        long start = System.nanoTime();
        request("pl-PL")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price.status").value("UNAVAILABLE"));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(1_000);
    }

    @Test
    void upstreamsAreCalledInParallel() throws Exception {
        request("pl-PL");
        stubCatalog(okJson("""
                {"name": "Filtr oleju", "description": "", "specs": {}, "images": []}""").withFixedDelay(100));
        stubPricing(okJson("{\"currency\": \"PLN\"}").withFixedDelay(100));
        stubStock(okJson("{\"quantity\": 1, \"warehouse\": \"Poznań\", \"deliveryInDays\": 1}").withFixedDelay(100));

        long start = System.nanoTime();
        request("pl-PL").andExpect(status().isOk());
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(250);
    }

    @Test
    void unknownMarketIs400() throws Exception {
        request("xx-XX").andExpect(status().isBadRequest());
    }

    @Test
    void blankCustomerIdIs400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/products/P-1001").param("market", "pl-PL").param("customerId", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openCircuitStopsCallingPricing() throws Exception {
        stubPricing(aResponse().withStatus(500));

        request("pl-PL");
        request("pl-PL");
        request("pl-PL").andExpect(status().isOk()).andExpect(jsonPath("$.price.status").value("UNAVAILABLE"));
        request("pl-PL").andExpect(status().isOk()).andExpect(jsonPath("$.price.status").value("UNAVAILABLE"));

        upstreams.verify(exactly(4), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
    }

    @Test
    void rejectedRequestIsNotRetriedAndKeepsTheCircuitClosed() throws Exception {
        stubPricing(aResponse().withStatus(400));

        for (int i = 0; i < 4; i++) {
            request("pl-PL").andExpect(status().isOk()).andExpect(jsonPath("$.price.status").value("UNAVAILABLE"));
        }

        upstreams.verify(exactly(4), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
        assertThat(circuitBreakers.circuitBreaker("pricing").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void fastErrorIsRetriedOnce() throws Exception {
        upstreams.stubFor(get(urlPathEqualTo("/prices/P-1001")).inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED).willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));
        upstreams.stubFor(get(urlPathEqualTo("/prices/P-1001")).inScenario("retry")
                .whenScenarioStateIs("recovered")
                .willReturn(okJson("{\"currency\": \"PLN\", \"finalPrice\": 110.70}")));

        request("pl-PL").andExpect(jsonPath("$.price.status").value("AVAILABLE"));

        upstreams.verify(exactly(2), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
    }

    @Test
    void timeoutIsNotRetried() throws Exception {
        stubPricing(okJson("{}").withFixedDelay(1_000));

        request("pl-PL").andExpect(jsonPath("$.price.status").value("UNAVAILABLE"));

        upstreams.verify(exactly(1), getRequestedFor(urlPathEqualTo("/prices/P-1001")));
    }

    @Test
    void responseCarriesTheTraceId() throws Exception {
        request("pl-PL").andExpect(header().exists("X-Trace-Id"));
    }

    @Test
    void upstreamFailuresAreCountedByReason() throws Exception {
        stubPricing(aResponse().withStatus(500));
        double before = meters.counter("aggregator.upstream.failures", "upstream", "pricing", "reason", "ERROR").count();

        request("pl-PL");

        assertThat(meters.counter("aggregator.upstream.failures", "upstream", "pricing", "reason", "ERROR").count())
                .isEqualTo(before + 2);
    }

    @Test
    void degradedSectionIsCounted() throws Exception {
        stubPricing(aResponse().withStatus(500));
        double before = meters.counter("aggregator.sections.degraded", "section", "price").count();

        request("pl-PL");

        assertThat(meters.counter("aggregator.sections.degraded", "section", "price").count()).isEqualTo(before + 1);
    }
}
