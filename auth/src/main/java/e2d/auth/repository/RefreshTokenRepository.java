package e2d.auth.repository;

import e2d.auth.domain.RefreshToken;
import e2d.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    void deleteByToken(String refreshToken);
    void deleteAllByUser(User user);

    @Query("""
    select rt from RefreshToken rt
    join fetch rt.user
    where rt.token = :token
""")
    Optional<RefreshToken> findByTokenWithUser(@Param("token") String token);

    Optional<RefreshToken> findByToken(String refreshToken);
}
