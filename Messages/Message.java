package Messages;
import java.util.Arrays;
import java.util.HashMap;
import java.net.Socket;

/**
 * Represents a generic HTTP message. The Message class is designed to be a base class
 * for HTTP-based request and response messages. It encapsulates common elements
 * such as HTTP version, headers, body, socket, and an associated transaction, allowing
 * subclasses to implement additional specific behaviors and attributes.
 *
 * The Message class is abstract, meaning it cannot be instantiated directly. It provides
 * utility methods for accessing and manipulating message components and requires subclasses
 * to define their own implementation for the `toString` method.
 *
 * Key features:
 * - Stores HTTP version, headers, body, and socket details.
 * - Provides methods to access and manipulate header information and message content.
 * - Supports an associated Transaction object to pair requests with corresponding responses.
 */
public abstract class Message {
    protected String httpVersion;
    protected HashMap<String, String> headers;
    protected byte[] body;
    protected Socket socket;
    protected Transaction txn;

    /**
     * Default constructor for the Message class. Initializes a generic HTTP message
     * with default values:
     * - The HTTP version is set to an empty string.
     * - The headers are initialized as an empty HashMap.
     * - The body is set to an empty string.
     * - The socket is set to null.
     *
     * This constructor is intended to provide a basic initialization for subclasses
     * or testing purposes, where custom values can be set after object creation.
     */
    public Message() {
        httpVersion = "";
        headers = new HashMap<>();
        body = new byte[0];
        socket = null;
    }

    /**
     * Constructs a Message object with the specified HTTP version, headers, body, and socket.
     *
     * @param httpVersion the HTTP version of the message (e.g., HTTP/1.1, HTTP/2)
     * @param headers a map containing the headers of the message as key-value pairs
     * @param body the body of the message, or an empty string if not present
     * @param socket the socket associated with the client-server connection for this message
     */
    public Message(String httpVersion, HashMap<String, String> headers, String body, Socket socket) {
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.body = body.getBytes();
        this.socket = socket;
    }

    public Message(String httpVersion, HashMap<String, String> headers, byte[] body, Socket socket) {
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.body = body;
        this.socket = socket;
    }

    /**
     * Retrieves the HTTP version of the message.
     *
     * @return the HTTP version as a String (e.g., HTTP/1.1, HTTP/2)
     */
    public String getHTTPVersion() {
        return httpVersion;
    }

    /**
     * Retrieves the headers of the HTTP message.
     *
     * @return a map containing the headers of the message as key-value pairs
     */
    public HashMap<String, String> getHeaders() {
        return headers;
    }

    /**
     * Retrieves the body of the message.
     *
     * @return the body of the message as a string, or an empty string if no body is present
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * Retrieves the socket associated with the message.
     *
     * @return the socket used for the client-server connection, or null if no socket is set
     */
    public Socket getSocket() { 
        return socket;
    }

    /**
     * Constructs a string representation of the message. The specific format and content
     * of the string are determined by subclasses, such as Request or Response, to reflect
     * the details of the message, including components like headers, body, and protocol details.
     *
     * @return a formatted string representing the message, including its specific attributes
     * as implemented by subclasses.
     */
    public abstract String toString();

    /**
     * Appends the message tail by constructing a string representation of the headers
     * and the body of the message, if present. The headers are formatted as
     * "key: value" pairs separated by a line break, followed by a blank line. If
     * the body is not null or empty, it is appended after the headers.
     *
     * @return a string representation of the message tail, including headers and the body if any
     */
    protected byte[] appendMessageTail() {
        StringBuilder sb = new StringBuilder();
        //get headers
        for (HashMap.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        //whitespace at end of headers
        sb.append("\r\n");

        //turn it into bytes
        byte[] tail = sb.toString().getBytes();
        //append body if one exists
        if (body != null && body.length > 0) {
            tail = Arrays.copyOf(tail, tail.length + body.length);
            System.arraycopy(body, 0, tail, tail.length - body.length, body.length);
        }

        //return completed Message
        return tail;
    }

    /**
     * Retrieves the Transaction object associated with this Message
     * @return the associated Transaction
     */
    public Transaction getTransaction() {
        return txn;
    }
}