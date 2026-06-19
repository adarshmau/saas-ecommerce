package com.saas.ecommerce.config;

import com.saas.ecommerce.auth.JwtService;
import com.saas.ecommerce.auth.TokenBlacklistService;
import com.saas.ecommerce.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter  extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService; // ✅ NEW


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        //Explicit null check first
        if (authHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        //  Then check Bearer prefix
            if (!authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        //  Is token blacklisted (logged out)?
        if (tokenBlacklistService.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401," +
                            "\"message\":\"Token invalidated. Please login again.\"}");
            return;
        }


        var claims = jwtService.extractAllClaims(token);
        String email = claims.getSubject();
        String role =  claims.get("role",String.class);
        String tenantId = claims.get("tenantId",String.class);

        //Set tenant context for multi-tenancy
        TenantContext.setTenantId(tenantId);

        //Set Authentication in spring Security
        UsernamePasswordAuthenticationToken authentication=
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_"+role))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);


    }
}
