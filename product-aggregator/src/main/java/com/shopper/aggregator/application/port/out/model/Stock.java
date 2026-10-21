package com.shopper.aggregator.application.port.out.model;

public record Stock(int quantity, String warehouse, int deliveryInDays) {
}
