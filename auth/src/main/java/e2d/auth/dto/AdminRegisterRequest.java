package e2d.auth.dto;

import java.io.Serializable;
import java.util.Set;

public record AdminRegisterRequest(String username, String password, Set<String> roles) implements Serializable {
}
