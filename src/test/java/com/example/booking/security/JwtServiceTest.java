package com.example.booking.security;

import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User sampleUser() {
        return User.builder()
                .id(1L)
                .username("user@example.com")
                .password("hashed")
                .role(Role.USER)
                .build();
    }

    @Test
    void generateToken_thenExtractUsername_matches() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        assertEquals("user@example.com", jwtService.extractUsername(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_forCorrectUser_returnsTrue() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_forDifferentUser_returnsFalse() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        User otherUser = User.builder()
                .id(2L)
                .username("other@example.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_forExpiredToken_returnsFalse() {
        // Temporarily set expiration to a negative value so the token is already expired.
        Object originalExpiration = ReflectionTestUtils.getField(jwtService, "expirationMs");
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);

        User user = sampleUser();
        String token = jwtService.generateToken(user);

        ReflectionTestUtils.setField(jwtService, "expirationMs", originalExpiration);

        assertFalse(jwtService.isTokenValid(token, user));
    }
}
