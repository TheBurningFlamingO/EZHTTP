package Data;
import java.lang.IllegalArgumentException;

/**
 * The {@code ResponseCode} enum represents standard HTTP response codes
 * along with associated messages. Each response code is a numeric value
 * defined by HTTP standards, accompanied by a brief description.
 *
 * This enum provides utility methods to retrieve the response code as an
 * integer, get the corresponding message, and format the response code
 * and message as a string.
 */
public enum ResponseCode {
    OK(200, "OK"),
    CREATED(201, "Created"),
    NO_CONTENT(204, "No Content"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
    EXPECTATION_FAILED(417, "Expectation Failed"),
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public boolean isError() {
        return (code >= 400);
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

    /**
     * Retrieves the {@code ResponseCode} corresponding to the given numeric HTTP response code.
     *
     * @param code the numeric HTTP response code to match
     * @return the corresponding {@code ResponseCode} instance
     * @throws IllegalArgumentException if no matching {@code ResponseCode} is found for the given code
     */
    public static ResponseCode fromCode(int code) throws IllegalArgumentException {
        for (ResponseCode rc : values()) {
            if (rc.getCode() == code)
                return rc;
        }

        throw new IllegalArgumentException("Unknown response code: " + code + "!");
    }
}
