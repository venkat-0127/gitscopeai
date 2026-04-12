package com.lms.Digital_library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOrderResponse {
    private String orderId;
    private String keyId;
    private long amountPaise;   // Razorpay needs paise
    private String currency;    // "INR"
    private String receipt;
}
