package com.shopper.aggregator.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentifierTest {

    @Test
    void identifiersWithTheSameValueAreEqual() {
        assertThat(new ProductId("P-1001")).isEqualTo(new ProductId("P-1001"));
        assertThat(new CustomerId("C-42")).isEqualTo(new CustomerId("C-42"));
    }

    @Test
    void blankIdentifierCannotBeCreated() {
        assertThatThrownBy(() -> new ProductId(" ")).isInstanceOf(InvalidIdentifierException.class);
        assertThatThrownBy(() -> new CustomerId(null)).isInstanceOf(InvalidIdentifierException.class);
    }
}
