package com.lms.Digital_library.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class CreateOrderRequest {
    @NotNull
    @DecimalMin(value = "1.00") // rupees
    private BigDecimal amountRupees;

    private Long borrowId;            // optional: fine for a borrow

    private String payerName;         // display only
    @Email
    private String payerEmail;        // maps to Payment.payerEmail

    private String receipt;           // optional; stored in Payment.reference

    private Map<String, Object> notes; // optional metadata (sent to Razorpay)
}
