package com.example.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private String username;
    private String role;

    public LoginResponse(String token, String username, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.username = username;
        this.role = role;
    }
}
