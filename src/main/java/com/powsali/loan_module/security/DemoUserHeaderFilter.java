package com.powsali.loan_module.security;

import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Local-only identity bridge for the demo account switcher.
 * Never register this filter in production: the demo header is not proof of identity.
 */
@Component
@Profile("local-demo")
@RequiredArgsConstructor
public class DemoUserHeaderFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || ("GET".equalsIgnoreCase(request.getMethod())
                && "/api/demo/users".equals(request.getServletPath()))
                || "/api/auth/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawUserId = request.getHeader("X-Demo-User-Id");
            if (rawUserId == null || rawUserId.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "Select a local demo user before accessing this endpoint.");
                return;
            }

            final Long userId;
            try {
                userId = Long.valueOf(rawUserId);
            } catch (NumberFormatException exception) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "X-Demo-User-Id must be a valid numeric user ID.");
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getUsername() == null
                    || !(user.getUsername().matches("(employee|ddo|hoo|support)\\d+")
                    || "admin".equals(user.getUsername()))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                        "The selected local demo user does not exist.");
                return;
            }

            CustomUserDetails principal = new CustomUserDetails(user);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
