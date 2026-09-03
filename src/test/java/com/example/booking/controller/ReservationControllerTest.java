package com.example.booking.controller;

import com.example.booking.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerTest extends BaseIntegrationTest {

    private Long firstResourceId() throws Exception {
        String token = adminToken();
        String body = mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("id").asLong();
    }

    private Long createReservation(String token, Long resourceId, String start, String end, String price) throws Exception {
        String body = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"%s","endTime":"%s","price":%s}
                                """.formatted(resourceId, start, end, price)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    void createReservation_asUser_setsOwnerFromJwt_notFromBody() throws Exception {
        String token = userToken();
        Long resourceId = firstResourceId();

        // Note: request body deliberately has no userId field to send - the DTO doesn't support one.
        String body = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-01T10:00:00","endTime":"2026-11-01T12:00:00","price":200.00}
                                """.formatted(resourceId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String username = objectMapper.readTree(body).get("username").asText();
        org.junit.jupiter.api.Assertions.assertEquals(USER_USERNAME, username);
    }

    @Test
    void userCannotAccessAnotherUsersReservation() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();
        Long resourceId = firstResourceId();

        // ADMIN creates a reservation for themselves
        Long adminReservationId = createReservation(adminTok, resourceId,
                "2026-11-02T10:00:00", "2026-11-02T12:00:00", "300.00");

        // USER tries to fetch admin's reservation by ID
        mockMvc.perform(get("/api/reservations/" + adminReservationId)
                        .header("Authorization", "Bearer " + userTok))
                .andExpect(status().isForbidden());
    }

    @Test
    void userSeesOnlyOwnReservationsInList() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();
        Long resourceId = firstResourceId();

        createReservation(userTok, resourceId, "2026-11-03T09:00:00", "2026-11-03T10:00:00", "50.00");
        createReservation(adminTok, resourceId, "2026-11-03T11:00:00", "2026-11-03T12:00:00", "50.00");

        String body = mockMvc.perform(get("/api/reservations?size=100")
                        .header("Authorization", "Bearer " + userTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var content = objectMapper.readTree(body).get("content");
        for (var node : content) {
            org.junit.jupiter.api.Assertions.assertEquals(USER_USERNAME, node.get("username").asText());
        }
    }

    @Test
    void adminSeesAllReservationsInList() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();
        Long resourceId = firstResourceId();

        createReservation(userTok, resourceId, "2026-11-04T09:00:00", "2026-11-04T10:00:00", "50.00");
        createReservation(adminTok, resourceId, "2026-11-04T11:00:00", "2026-11-04T12:00:00", "50.00");

        String body = mockMvc.perform(get("/api/reservations?size=100")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int totalElements = objectMapper.readTree(body).get("totalElements").asInt();
        org.junit.jupiter.api.Assertions.assertTrue(totalElements >= 2);
    }

    @Test
    void overlappingReservation_isRejectedWithConflict() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();

        createReservation(userTok, resourceId, "2026-11-05T10:00:00", "2026-11-05T12:00:00", "100.00");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-05T11:00:00","endTime":"2026-11-05T13:00:00","price":100.00}
                                """.formatted(resourceId)))
                .andExpect(status().isConflict());
    }

    @Test
    void backToBackReservation_isAllowed() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();

        createReservation(userTok, resourceId, "2026-11-06T10:00:00", "2026-11-06T12:00:00", "100.00");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-06T12:00:00","endTime":"2026-11-06T14:00:00","price":100.00}
                                """.formatted(resourceId)))
                .andExpect(status().isCreated());
    }

    @Test
    void endTimeBeforeStartTime_returns400() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-07T12:00:00","endTime":"2026-11-07T10:00:00","price":100.00}
                                """.formatted(resourceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void negativePrice_returns400() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-08T10:00:00","endTime":"2026-11-08T12:00:00","price":-10.00}
                                """.formatted(resourceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonexistentResourceId_returns404() throws Exception {
        String userTok = userToken();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":999999,"startTime":"2026-11-09T10:00:00","endTime":"2026-11-09T12:00:00","price":10.00}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotUpdateOrDeleteReservations() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();
        Long reservationId = createReservation(userTok, resourceId,
                "2026-11-10T10:00:00", "2026-11-10T12:00:00", "80.00");

        mockMvc.perform(put("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-10T10:00:00","endTime":"2026-11-10T12:00:00","price":999.00,"status":"CONFIRMED"}
                                """.formatted(resourceId)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + userTok))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateReservationStatus() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();
        Long resourceId = firstResourceId();
        Long reservationId = createReservation(userTok, resourceId,
                "2026-11-11T10:00:00", "2026-11-11T12:00:00", "80.00");

        mockMvc.perform(put("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-11T10:00:00","endTime":"2026-11-11T12:00:00","price":80.00,"status":"CONFIRMED"}
                                """.formatted(resourceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void invalidStatusValue_returns400() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();
        Long resourceId = firstResourceId();
        Long reservationId = createReservation(userTok, resourceId,
                "2026-11-12T10:00:00", "2026-11-12T12:00:00", "80.00");

        mockMvc.perform(put("/api/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType("application/json")
                        .content("""
                                {"resourceId":%d,"startTime":"2026-11-12T10:00:00","endTime":"2026-11-12T12:00:00","price":80.00,"status":"NOT_A_STATUS"}
                                """.formatted(resourceId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterByStatus_returnsOnlyMatchingReservations() throws Exception {
        String userTok = userToken();
        String adminTok = adminToken();
        Long resourceId = firstResourceId();

        Long id = createReservation(userTok, resourceId, "2026-11-13T10:00:00", "2026-11-13T12:00:00", "80.00");
        mockMvc.perform(put("/api/reservations/" + id)
                .header("Authorization", "Bearer " + adminTok)
                .contentType("application/json")
                .content("""
                        {"resourceId":%d,"startTime":"2026-11-13T10:00:00","endTime":"2026-11-13T12:00:00","price":80.00,"status":"CONFIRMED"}
                        """.formatted(resourceId)));

        String body = mockMvc.perform(get("/api/reservations?status=CONFIRMED&size=100")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var content = objectMapper.readTree(body).get("content");
        for (var node : content) {
            org.junit.jupiter.api.Assertions.assertEquals("CONFIRMED", node.get("status").asText());
        }
    }

    @Test
    void filterByPriceRange_returnsOnlyMatchingReservations() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();

        createReservation(userTok, resourceId, "2026-11-14T10:00:00", "2026-11-14T11:00:00", "45.00");
        createReservation(userTok, resourceId, "2026-11-14T12:00:00", "2026-11-14T13:00:00", "999.00");

        String body = mockMvc.perform(get("/api/reservations?minPrice=500&maxPrice=1500&size=100")
                        .header("Authorization", "Bearer " + userTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var content = objectMapper.readTree(body).get("content");
        for (var node : content) {
            double price = node.get("price").asDouble();
            org.junit.jupiter.api.Assertions.assertTrue(price >= 500 && price <= 1500);
        }
    }

    @Test
    void pagination_respectsPageAndSizeParams() throws Exception {
        String token = userToken();
        String body = mockMvc.perform(get("/api/reservations?page=0&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(body);
        org.junit.jupiter.api.Assertions.assertEquals(0, json.get("page").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(2, json.get("size").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(json.get("content").size() <= 2);
    }

    @Test
    void sortingByPriceDescending_returnsSortedResults() throws Exception {
        String userTok = userToken();
        Long resourceId = firstResourceId();

        createReservation(userTok, resourceId, "2026-11-15T08:00:00", "2026-11-15T09:00:00", "10.00");
        createReservation(userTok, resourceId, "2026-11-15T10:00:00", "2026-11-15T11:00:00", "500.00");

        String body = mockMvc.perform(get("/api/reservations?sort=price,desc&size=100")
                        .header("Authorization", "Bearer " + userTok))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var content = objectMapper.readTree(body).get("content");
        double previous = Double.MAX_VALUE;
        for (var node : content) {
            double price = node.get("price").asDouble();
            org.junit.jupiter.api.Assertions.assertTrue(price <= previous);
            previous = price;
        }
    }
}
