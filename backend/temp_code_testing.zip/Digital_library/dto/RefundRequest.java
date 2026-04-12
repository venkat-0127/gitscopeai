package com.lms.Digital_library.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefundRequest {
    @NotBlank
    private String razorpayPaymentId;

    /** 0 or missing => full refund */
    @Min(0)
    private long amountPaise;
}
