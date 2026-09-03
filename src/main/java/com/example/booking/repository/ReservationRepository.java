package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    /**
     * Finds reservations for the same resource that overlap the given time window
     * and are not cancelled. Two intervals [s1,e1) and [s2,e2) overlap when
     * s1 < e2 AND s2 < e1. CANCELLED reservations do not block new bookings.
     *
     * @param excludeReservationId reservation id to exclude from the check (used on updates); pass -1 when creating
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status <> :cancelledStatus
              AND r.id <> :excludeReservationId
              AND r.startTime < :endTime
              AND :startTime < r.endTime
            """)
    List<Reservation> findOverlapping(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeReservationId") Long excludeReservationId,
            @Param("cancelledStatus") ReservationStatus cancelledStatus
    );
}
