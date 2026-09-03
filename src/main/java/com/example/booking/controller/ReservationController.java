package com.example.booking.controller;

import com.example.booking.dto.PageResponse;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.entity.User;
import com.example.booking.exception.InvalidRequestException;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reservation creation and management")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Create a reservation. Owner is always derived from the JWT.")
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request, currentUser));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "List reservations. ADMIN sees all; USER sees only their own. Supports filtering, pagination, sorting.")
    public ResponseEntity<PageResponse<ReservationResponse>> list(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Filter by status: PENDING, CONFIRMED, CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @PageableDefault(size = 10) Pageable pageable) {

        ReservationStatus parsedStatus = parseStatusOrNull(status);
        return ResponseEntity.ok(reservationService.list(currentUser, parsedStatus, minPrice, maxPrice, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Operation(summary = "Get a reservation by id. USER may only access their own reservation.")
    public ResponseEntity<ReservationResponse> getById(@PathVariable Long id,
                                                         @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(reservationService.getById(id, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a reservation, including status (ADMIN only)")
    public ResponseEntity<ReservationResponse> update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ReservationStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ReservationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid status filter value: " + status);
        }
    }
}
