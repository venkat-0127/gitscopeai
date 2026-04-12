// src/main/java/com/lms/Digital_library/dto/VerifyPaymentRequest.java
package com.lms.Digital_library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyPaymentRequest {

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;

    @NotNull
    private Integer borrowId;

    // ✅ Make this OPTIONAL (no @NotNull)
    private Integer amountPaise; // optional; send if you want to freeze fine = amountPaise/100
}
