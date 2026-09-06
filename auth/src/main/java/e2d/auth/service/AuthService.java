package e2d.auth.service;

import e2d.auth.domain.RefreshToken;
import e2d.auth.domain.Role;
import e2d.auth.domain.User;
import e2d.auth.dto.AdminRegisterRequest;
import e2d.auth.dto.AuthResponse;
import e2d.auth.dto.LoginRequest;
import e2d.auth.dto.RegisterRequest;
import e2d.auth.exception.InvalidCredentialsException;
import e2d.auth.exception.RoleNotFoundException;
import e2d.auth.exception.UserNotFoundException;
import e2d.auth.repository.RoleRepository;
import e2d.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RoleRepository roleRepository;

    public AuthResponse register(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.username());
        user.setEmail(registerRequest.email());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RoleNotFoundException("ROLE_USER"));
        user.setRoles(Set.of(role));
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse adminRegister(AdminRegisterRequest request) {
        Set<Role> roles = request.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RoleNotFoundException(roleName)))
                .collect(Collectors.toSet());
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByUsernameWithRoles(loginRequest.username())
                .orElseThrow(() -> new UserNotFoundException(loginRequest.username()));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        refreshTokenService.revokeAll(user);
    }
}
