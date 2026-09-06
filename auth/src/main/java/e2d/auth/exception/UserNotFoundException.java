package e2d.auth.exception;

public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
}
