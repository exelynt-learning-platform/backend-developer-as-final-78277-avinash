package com.example.booking.config;

import com.example.booking.entity.Resource;
import com.example.booking.entity.Role;
import com.example.booking.entity.User;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds test users and sample resources on startup, for local development
 * and evaluation only. Passwords are BCrypt-hashed before persistence.
 * Seeding is idempotent - it only runs when the tables are empty.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.save(User.builder()
                .username("admin@example.com")
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .build());

        userRepository.save(User.builder()
                .username("user@example.com")
                .password(passwordEncoder.encode("User@123"))
                .role(Role.USER)
                .build());
    }

    private void seedResources() {
        if (resourceRepository.count() > 0) {
            return;
        }

        resourceRepository.save(Resource.builder()
                .name("Conference Room A")
                .description("Large conference room with projector")
                .type("ROOM")
                .available(true)
                .price(new BigDecimal("1000.00"))
                .build());

        resourceRepository.save(Resource.builder()
                .name("Meeting Room B")
                .description("Small meeting room, seats 6")
                .type("ROOM")
                .available(true)
                .price(new BigDecimal("500.00"))
                .build());

        resourceRepository.save(Resource.builder()
                .name("Company Vehicle")
                .description("Sedan for business travel")
                .type("VEHICLE")
                .available(true)
                .price(new BigDecimal("1500.00"))
                .build());

        resourceRepository.save(Resource.builder()
                .name("Projector")
                .description("Portable HD projector")
                .type("EQUIPMENT")
                .available(true)
                .price(new BigDecimal("200.00"))
                .build());

        resourceRepository.save(Resource.builder()
                .name("Laptop")
                .description("Loaner laptop for presentations")
                .type("EQUIPMENT")
                .available(true)
                .price(new BigDecimal("150.00"))
                .build());
    }
}
