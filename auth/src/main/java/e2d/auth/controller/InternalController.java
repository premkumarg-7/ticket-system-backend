package e2d.auth.controller;

import e2d.auth.domain.User;
import e2d.auth.dto.UserInfoResponse;
import e2d.auth.exception.UserNotFoundException;
import e2d.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalController {

    private final UserRepository userRepository;

    @GetMapping("/users/{username}")
    public ResponseEntity<UserInfoResponse> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        return ResponseEntity.ok(new UserInfoResponse(user.getUsername(), user.getEmail()));
    }

    @GetMapping("/users/email/{email}")
    public Boolean CheckUserEmailExists(@PathVariable String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
