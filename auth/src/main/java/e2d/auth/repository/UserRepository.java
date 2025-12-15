package e2d.auth.repository;

import e2d.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {


    Optional<User> findByUsername(String username);

    @Query("""
        select u from User u
        left join fetch u.roles
        where u.username = :username
    """)
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

}
