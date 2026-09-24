package com.rikkeibank.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank(message = "Số tài khoản nguồn không được để trống")
    private String fromAccountNumber;

    @NotBlank(message = "Số tài khoản đích không được để trống")
    private String toAccountNumber;

    @NotNull(message = "Số tiền chuyển không được để trống")
    @DecimalMin(value = "1000.0", message = "Số tiền chuyển tối thiểu là 1,000 VND")
    private BigDecimal amount;

    private String description;

    // Optional flag to simulate credit failure during demo of Saga rollback
    private boolean simulateFailure;
}
