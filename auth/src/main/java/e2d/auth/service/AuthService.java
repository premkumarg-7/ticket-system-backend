package e2d.auth.service;

import e2d.auth.domain.RefreshToken;
import e2d.auth.domain.Role;
import e2d.auth.domain.User;
import e2d.auth.dto.AdminRegisterRequest;
import e2d.auth.dto.AuthResponse;
import e2d.auth.dto.LoginRequest;
import e2d.auth.dto.RegisterRequest;
import e2d.auth.repository.RoleRepository;
import e2d.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository _userRepo;
    private final PasswordEncoder _passwordEncoder;
    private final JwtService _jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RoleRepository roleRepository;

    public AuthResponse register(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.username());
        user.setPassword(_passwordEncoder.encode(registerRequest.password()));
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(Set.of(role));
        _userRepo.save(user);

        String accessToken = _jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse adminRegister(AdminRegisterRequest request){
        Set<Role> roles = request.roles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() ->new RuntimeException("Role not found: "+ roleName)))
                .collect(Collectors.toSet());
        User user = new User();
        user.setUsername(request.username());
        user.setPassword(_passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        _userRepo.save(user);

        String accessToken = _jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }



    public AuthResponse login(LoginRequest loginRequest) {
        User user = _userRepo.findByUsernameWithRoles(loginRequest.username())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!_passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }
        String accessToken = _jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user);

        return new AuthResponse(accessToken, refreshToken.getToken());
    }

}

