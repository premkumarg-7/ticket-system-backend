package e2d.auth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue
    @Column(length = 36)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name; // ROLE_USER, ROLE_ADMIN, ROLE_AGENT
}
