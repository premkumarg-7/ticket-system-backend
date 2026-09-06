package e2d.ticketService.Client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${app.auth-service.url}")
public interface AuthServiceClient {

    @GetMapping("/internal/users/{username}")
    UserInfoResponse getEmailByUsername(@PathVariable String username);

    @GetMapping("/internal/users/email/{email}")
    Boolean checkUserEmailExist(@PathVariable String email);

    record UserInfoResponse(String username, String email) {}
}
