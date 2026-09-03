package com.example.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Used for both reservation creation (USER/ADMIN) and admin updates.
 * The `status` field is ignored on create (new reservations always start PENDING)
 * and is required to be a valid enum value when present on update.
 * Note: this DTO intentionally has no `userId` field — ownership is always
 * derived from the authenticated JWT, never from client input.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must not be negative")
    private BigDecimal price;

    /** Only honored on admin updates; ignored on create. */
    private String status;
}
