package e2d.auth.service;

import e2d.auth.domain.RefreshToken;
import e2d.auth.domain.User;
import e2d.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken create(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plus(30, ChronoUnit.DAYS));
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenWithUser(token).orElseThrow(()-> new RuntimeException("Refresh token not found"));
        if(refreshToken.isRevoked()) throw new RuntimeException("Refresh token is revoked");

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        refreshToken.getUser().getUsername();

        return refreshToken;
    }

    public void revokeAll(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }
}
