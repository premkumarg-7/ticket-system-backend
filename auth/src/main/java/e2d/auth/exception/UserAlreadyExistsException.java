package e2d.auth.exception;

public class UserAlreadyExistsException extends AuthException {
    public UserAlreadyExistsException(String username) {
        super("User already exists: " + username);
    }
}
