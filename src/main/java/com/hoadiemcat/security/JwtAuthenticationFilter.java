package com.hoadiemcat.security;

import com.hoadiemcat.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                io.jsonwebtoken.Claims claims = tokenProvider.getClaimsFromJwt(jwt);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = java.util.Collections.emptyList();
                if (StringUtils.hasText(role)) {
                    String roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(roleAuthority));
                } else if (StringUtils.hasText(username)) {
                    try {
                        org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        authorities = userDetails.getAuthorities();
                    } catch (Exception e) {
                        log.warn("Could not load user details for username: {}", username);
                    }
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AppConstants.HEADER_STRING);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AppConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(AppConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
