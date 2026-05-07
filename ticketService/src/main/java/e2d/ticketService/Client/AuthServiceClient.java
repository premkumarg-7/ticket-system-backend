package e2d.ticketService.Client;

import e2d.ticketService.Exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    public String getEmailByUsername(String username) {
        String url = authServiceUrl + "/internal/users/" + username;
        try {
            UserInfoResponse response = restTemplate.getForObject(url, UserInfoResponse.class);
            if (response != null && response.email() != null) {
                return response.email();
            }
            throw new UserNotFoundException("User not found: " + username);
        } catch (Exception e) {
            log.error("Failed to fetch user email from auth service for username {}: {}", username, e.getMessage());
            throw new UserNotFoundException("Failed to fetch user email for: " + e);
        }
    }

    public boolean checkUserEmailExist(String email) {
        String url = authServiceUrl + "/internal/users/email/" + UriUtils.encode(email, StandardCharsets.UTF_8);
        try {
            Boolean response = restTemplate.exchange(url, HttpMethod.GET, null, Boolean.class).getBody();
            return response != null && response;
        } catch (Exception e) {
            log.error("Failed to check email existence for {}: {}", email, e.getMessage());
            throw new UserNotFoundException("Failed to check email existence for: " + e);
        }
    }

    public record UserInfoResponse(String username, String email) {
    }

}
