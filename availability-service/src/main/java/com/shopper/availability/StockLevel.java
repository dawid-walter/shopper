package com.shopper.availability;

public record StockLevel(String productId, String market, int quantity, String warehouse, int deliveryInDays) {
}
