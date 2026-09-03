package com.example.booking.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the JWT on every request (stateless auth - no HTTP session).
 * On success, populates the SecurityContext with the authenticated user so
 * downstream controllers/services can resolve identity from
 * SecurityContextHolder rather than trusting any client-supplied user id.
 *
 * On any failure (missing, malformed, expired, or otherwise invalid token,
 * or a token for a user that no longer exists) the filter simply leaves the
 * SecurityContext empty and lets the request continue. Spring Security's
 * authorization check then denies it, and JwtAuthenticationEntryPoint turns
 * that into a consistent 401 JSON body - so there's a single place that
 * formats auth-failure responses instead of two.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
                // else: token invalid/expired - leave context empty, fall through below
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            // malformed/expired token, or the token's subject no longer exists -
            // leave the SecurityContext empty; the request is treated as unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}
