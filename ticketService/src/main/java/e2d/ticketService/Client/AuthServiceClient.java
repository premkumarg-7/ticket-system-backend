package e2d.ticketService.Client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.auth-service.url:http://localhost:8081}")
    private String authServiceUrl;

    public String getEmailByUsername(String username) {
        String url = authServiceUrl + "/auth/users/" + username;
        try {
            UserInfoResponse response = restTemplate.getForObject(url, UserInfoResponse.class);
            if (response != null && response.email() != null) {
                return response.email();
            }
            throw new RuntimeException("User not found: " + username);
        } catch (Exception e) {
            log.error("Failed to fetch user email from auth service for username {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to fetch user email for: " + username, e);
        }
    }

    public record UserInfoResponse(String username, String email) {
    }
}
