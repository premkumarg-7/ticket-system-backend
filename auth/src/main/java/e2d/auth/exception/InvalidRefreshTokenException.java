package e2d.auth.exception;

public class InvalidRefreshTokenException extends AuthException {
    public InvalidRefreshTokenException(String reason) {
        super("Invalid refresh token: " + reason);
    }
}
