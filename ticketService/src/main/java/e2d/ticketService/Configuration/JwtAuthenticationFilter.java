package e2d.ticketService.Configuration;

import e2d.ticketService.Security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtService;

    public JwtAuthenticationFilter(JwtUtil jwtService) {
        this.jwtService = jwtService;
        System.err.println("JwtAuthenticationFilter: INITIALIZED");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            System.err.println("JwtAuthenticationFilter: Authorization header missing or invalid format");
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorizationHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {
            System.err.println("JwtAuthenticationFilter: Token validation returned false");
            filterChain.doFilter(request, response);
            return;
        }
        String user_id = jwtService.extractUserId(token);
        System.err.println("JwtAuthenticationFilter: Extracted User ID: " + user_id);

        if (user_id != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            List<String> roles = jwtService.extractRoles(token);
            if (roles == null) {
                System.err.println("JwtAuthenticationFilter: Roles claim is null or missing");
                roles = List.of();
            } else {
                System.err.println("JwtAuthenticationFilter: Extracted Roles: " + roles);
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .toList();
            System.out.println("Authorities" + authorities);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    user_id,
                    null,
                    authorities);

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            System.err.println("JwtAuthenticationFilter: Authentication set in SecurityContext");

        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return false;
    }
}
