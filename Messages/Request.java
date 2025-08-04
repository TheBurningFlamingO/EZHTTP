
package Messages;
import Tools.TxnLogger;

import java.net.Socket;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents an HTTP request, which is a type of message sent from a client to a server
 *  to request resources or to perform actions. Inherits common properties
 *  and behaviors from the Message class and adds HTTP-specific request features such as
 *  method and path.
 *
 * The class provides additional functionality to access the HTTP request method
 * and path, as well as a string representation of the entire request.
 */
public class Request extends Message {
    private final String method;
    private final String path;

    /**
     * Constructs a new Request object representing an HTTP request. The request includes
     * HTTP request-specific components such as method and path, in addition to the attributes
     * defined in the parent Message class.
     *
     * @param method the HTTP method for the request (e.g., GET, POST, PUT, DELETE)
     * @param path the requested resource path (e.g., "/index.html")
     * @param httpVersion the HTTP version of the request (e.g., HTTP/1.1)
     * @param headers a map containing the headers of the request as key-value pairs
     * @param body the body of the request, or an empty string if not present
     * @param socket the socket associated with the client-server connection for this request
     */
    public Request(String method, String path, String httpVersion, HashMap<String, String> headers, String body, Socket socket) {
        super(httpVersion, headers, body, socket);
        this.method = method;
        this.path = path;
        this.txn = new Transaction(this);
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

    /**
     * Retrieves the HTTP method of this request.
     *
     * @return the HTTP method as a String (e.g., GET, POST)
     */
    public String getMethod() {
        return method;

    }

    /**
     * Retrieves the path of the requested resource in this HTTP request.
     *
     * @return the resource path as a String
     */
    public String getPath() {
        return path;
    }

}


