
package Messages;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;

public class Request extends Message {
    private String method;
    private String path;
    public Request(String method, String path, String httpVersion, HashMap<String, String> headers, String body, Socket socket) {
        super(httpVersion, headers, body, socket);
        this.method = method;
        this.path = path;
    }

    /**
     * Generates a string representation of the HTTP request. The method and path
     * are included, followed by the HTTP version, headers, and body (if any).
     *
     * @return a formatted string representing the HTTP request, including the request line
     * and the headers and body from the message tail.
     */
    public String toString() {
        String s = method + " " + path + " " + httpVersion + "\r\n" + appendMessageTail();

        return s.trim();
    }
    public String getMethod() {
        return method;

    }
    public String getPath() {
        return path;
    }

}


