package com.aetherpmo.security;

import com.aetherpmo.adapter.amaranth.TokenValidator;
import com.aetherpmo.auth.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MockAuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenValidator tokenValidator;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(BEARER.length());
            tokenValidator.validate(token).ifPresent(userId ->
                    userRepository.findById(userId).ifPresent(user -> {
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                        var auth = new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    })
            );
        }

        filterChain.doFilter(request, response);
    }
}
