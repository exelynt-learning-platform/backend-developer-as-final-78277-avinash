package com.example.booking.dto;

import com.example.booking.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long userId;
    private String username;
    private Long resourceId;
    private String resourceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private String status;
    private LocalDateTime createdAt;

    public static ReservationResponse fromEntity(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUser().getId(),
                reservation.getUser().getUsername(),
                reservation.getResource().getId(),
                reservation.getResource().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPrice(),
                reservation.getStatus().name(),
                reservation.getCreatedAt()
        );
    }
}
