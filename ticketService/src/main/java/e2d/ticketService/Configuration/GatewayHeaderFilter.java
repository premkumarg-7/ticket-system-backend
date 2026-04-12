package e2d.ticketService.Configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Trusts requests from the API Gateway. The Gateway already validates the JWT
 * and forwards user identity as headers. This service does NOT re-validate
 * JWTs.
 *
 * Expected headers set by Gateway's JwtAuthFilter:
 * X-User-Name : authenticated username / subject
 * X-Roles : comma-separated roles e.g. "ROLE_USER,ROLE_ADMIN"
 */
@Component
@Slf4j
public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String username = request.getHeader("X-User-Name");
        String rolesHeader = request.getHeader("X-Roles");

        if (username != null && rolesHeader != null && !rolesHeader.isBlank()) {
            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> new SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .toList();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(username, null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("GatewayHeaderFilter: authenticated '{}' with roles {}", username, authorities);
        } else {
            log.debug("GatewayHeaderFilter: no gateway headers on path {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
