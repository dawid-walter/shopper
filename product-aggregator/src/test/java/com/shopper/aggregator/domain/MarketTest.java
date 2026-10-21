package com.shopper.aggregator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.shopper.aggregator.domain.exception.UnsupportedMarketException;

class MarketTest {

    @Test
    void supportedMarketIsCreated() {
        assertThat(new Market("pl-PL").code()).isEqualTo("pl-PL");
    }

    @Test
    void unsupportedMarketCannotBeCreated() {
        assertThatThrownBy(() -> new Market("xx-XX")).isInstanceOf(UnsupportedMarketException.class);
    }
}
