package e2d.auth.dto;

import e2d.auth.domain.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    @NotNull
    public String username;

    @NotNull
    public String password;

    public Role role;
}
