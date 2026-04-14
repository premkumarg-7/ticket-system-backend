package e2d.auth.exception;

public class RoleNotFoundException extends AuthException {
    public RoleNotFoundException(String roleName) {
        super("Role not found: " + roleName);
    }
}
