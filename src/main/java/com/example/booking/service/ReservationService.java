package com.example.booking.service;

import com.example.booking.dto.PageResponse;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.exception.InvalidRequestException;
import com.example.booking.exception.ReservationConflictException;
import com.example.booking.exception.ReservationNotFoundException;
import com.example.booking.exception.UnauthorizedException;
import com.example.booking.repository.ReservationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceService resourceService;

    /**
     * Creates a reservation for the authenticated user. `currentUser` must come
     * from the SecurityContext (JWT) in the controller layer - never from the
     * request body - so ownership can never be spoofed by the client.
     */
    @Transactional
    public ReservationResponse create(ReservationRequest request, User currentUser) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.findEntityById(request.getResourceId());

        checkNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), -1L);

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ReservationStatus.PENDING)
                .build();

        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    /**
     * Lists reservations with optional status/price filters, pagination, and sorting.
     * ADMIN sees everything matching the filter; USER sees only their own reservations.
     */
    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(User currentUser,
                                                    ReservationStatus status,
                                                    BigDecimal minPrice,
                                                    BigDecimal maxPrice,
                                                    Pageable pageable) {

        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        Specification<Reservation> spec = buildSpecification(
                isAdmin ? null : currentUser.getId(), status, minPrice, maxPrice
        );

        Page<Reservation> page = reservationRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(ReservationResponse::fromEntity));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getById(Long id, User currentUser) {
        Reservation reservation = findEntityById(id);
        enforceOwnership(reservation, currentUser);
        return ReservationResponse.fromEntity(reservation);
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationRequest request) {
        Reservation reservation = findEntityById(id);

        validateTimeRange(request.getStartTime(), request.getEndTime());

        Resource resource = resourceService.findEntityById(request.getResourceId());
        checkNoOverlap(resource.getId(), request.getStartTime(), request.getEndTime(), id);

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            reservation.setStatus(parseStatus(request.getStatus()));
        }

        return ReservationResponse.fromEntity(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ReservationNotFoundException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    // ---- helpers ----

    @Transactional(readOnly = true)
    public Reservation findEntityById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException("Reservation not found with id: " + id));
    }

    /**
     * Enforces that a USER can only access their own reservation. ADMIN bypasses
     * this check. This is the core anti-IDOR control: the reservation ID alone
     * is never sufficient to view data belonging to another user.
     */
    private void enforceOwnership(Reservation reservation, User currentUser) {
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = reservation.getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("You do not have permission to access this reservation");
        }
    }

    private void validateTimeRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null) {
            throw new InvalidRequestException("Start time and end time are required");
        }
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("End time must be after start time");
        }
    }

    private void checkNoOverlap(Long resourceId, java.time.LocalDateTime start, java.time.LocalDateTime end, Long excludeId) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(
                resourceId, start, end, excludeId, ReservationStatus.CANCELLED
        );
        if (!overlapping.isEmpty()) {
            throw new ReservationConflictException(
                    "The resource is already booked during the requested time window");
        }
    }

    private ReservationStatus parseStatus(String status) {
        try {
            return ReservationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid status value: " + status);
        }
    }

    private Specification<Reservation> buildSpecification(Long ownerId,
                                                            ReservationStatus status,
                                                            BigDecimal minPrice,
                                                            BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), ownerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
