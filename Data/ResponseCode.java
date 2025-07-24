package Data;
import java.lang.IllegalArgumentException;

public enum ResponseCode {
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
    public String toString() {
        return (code + " " + message).trim();
    }

    public static ResponseCode fromCode(int code) throws IllegalArgumentException {
        for (ResponseCode rc : values()) {
            if (rc.getCode() == code)
                return rc;
        }

        throw new IllegalArgumentException("Unknown response code: " + code + "!");
    }
}
