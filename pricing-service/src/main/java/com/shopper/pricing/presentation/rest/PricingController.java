package com.shopper.pricing.presentation.rest;

import java.time.Clock;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopper.pricing.application.port.in.PricingUseCase;
import com.shopper.pricing.domain.Market;
import com.shopper.pricing.domain.PricingContext;

@RestController
class PricingController {

    record VersionAdded(String componentId, int versions) {
    }

    private final PricingUseCase pricing;
    private final Clock clock;

    PricingController(PricingUseCase pricing, Clock clock) {
        this.pricing = pricing;
        this.clock = clock;
    }

    @GetMapping("/prices/{productId}")
    PriceResponse price(@PathVariable String productId,
                        @RequestParam String market,
                        @RequestParam(required = false) String customerId,
                        @RequestParam(required = false) Instant at) {
        var context = new PricingContext(productId, Market.of(market), customerId, at != null ? at : clock.instant());
        return PriceResponse.from(pricing.quote(context));
    }

    @PostMapping("/components/{componentId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    VersionAdded addVersion(@PathVariable String componentId, @RequestBody AddVersionRequest request) {
        var component = pricing.addVersion(componentId, request.toNewVersion());
        return new VersionAdded(component.id(), component.versions().size());
    }
}
