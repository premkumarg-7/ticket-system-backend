package e2d.auth.controller;

import e2d.auth.domain.RefreshToken;
import e2d.auth.domain.User;
import e2d.auth.dto.AuthResponse;
import e2d.auth.dto.LoginRequest;
import e2d.auth.dto.RegisterRequest;
import e2d.auth.dto.UserInfoResponse;
import e2d.auth.exception.UserNotFoundException;
import e2d.auth.repository.UserRepository;
import e2d.auth.service.AuthService;
import e2d.auth.service.JwtService;
import e2d.auth.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody @Valid RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        RefreshToken token = refreshTokenService.verify(refreshToken);
        String newAccess = jwtService.generateToken(token.getUser());
        return new AuthResponse(newAccess, refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal String username) {
        authService.logout(username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserInfoResponse> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        return ResponseEntity.ok(new UserInfoResponse(user.getUsername(), user.getEmail()));
    }
}
