package Messages;
import Data.ResponseCode;
import Tools.TxnLogger;

import java.util.*;
import java.net.Socket;

/**
 * The Response class represents an HTTP response message sent from a server to a client.
 * It extends the Message class and includes additional properties and methods specific
 * to a response, such as the HTTP response code and support for binary bodies.
 *
 * This class provides constructors to create a response either with a binary body or
 * a textual body, methods to retrieve the response details, and utilities to format
 * and convert the response into its byte representation.
 */
public class Response extends Message {
    //the response code
    private final String responseCode;

    //whether the response contains binary data
    private boolean isBinary = false;


    /**
     * Constructs a Response object using the provided request, HTTP version, response code,
     * headers, and binary body. This constructor primarily handles binary responses.
     *
     * @param request the originating Request object that this response is associated with
     * @param httpVersion the HTTP version of the response (e.g., HTTP/1.1)
     * @param rc the HTTP response code for the response, represented by a ResponseCode object
     * @param headers a map containing the headers of the response as key-value pairs
     * @param binaryBody the binary body of the response
     */
    public Response(Request request, String httpVersion, ResponseCode rc, HashMap<String, String> headers, byte[] binaryBody) {
        super(httpVersion, headers, "", request.getSocket());
        this.responseCode = rc.toString();
        this.body = binaryBody;
        this.isBinary = true;

    }

    /**
     * Checks if the response is in a binary format.
     *
     * @return true if the response is binary, false otherwise.
     */
    public boolean isBinary() {
        return isBinary;
    }

    /**
     * Constructs a Response object with the specified HTTP version, response code, headers, body,
     * and the originating request. Updates the associated transaction of the originating request
     * with this response and marks the transaction as complete.
     *
     * @param httpVersion the HTTP version of the response
     * @param responseCode the HTTP response code for the response
     * @param headers a map containing the headers of the response
     * @param body the body of the response, or an empty string if not present
     * @param origin the originating Request object from which this response is created
     */
    public Response(String httpVersion, String responseCode, HashMap<String, String> headers, String body,
    Request origin) {
        super(httpVersion, headers, body, origin.getSocket());
        this.responseCode = responseCode;
        this.txn = origin.getTransaction();

        //update txn with response
        txn.setResponse(this);
        txn.setComplete(true);
        TxnLogger.logTransaction(txn);
    }

    /**
     * Constructs a string representation of the HTTP response. The representation
     * consists of the HTTP version, response code, and the message tail. The message
     * tail includes the headers and body, formatted appropriately.
     *
     * @return a formatted string representing the HTTP response, including the status line,
     * headers, and body if present.
     */
    public String toString() {
        String s = httpVersion + " " + responseCode + "\r\n" +
                //get the message tail (same for all messages)
                new String(appendMessageTail()) + "\r\n";

        return s.trim();
    }
    public String getResponseCode() {
        return responseCode;
    }

    /**
     * Converts the current HTTP response, including the response line, headers, and body, into
     * a byte array representation. The resulting byte array can be used for data transmission
     * over a network or other I/O operations.
     *
     * The method performs the following steps:
     * 1. Constructs the response starting with the HTTP version, response code,
     *    and headers, each followed by a CRLF ("\r\n").
     * 2. Appends an additional CRLF to separate headers from the body.
     * 3. If a body is present, appends its byte representation to the resulting byte array.
     *
     * @return a byte array representing the HTTP response, including the status line, headers,
     *         and body (if present).
     */
    public byte[] getBytes() {
        StringBuilder sb = new StringBuilder().append(httpVersion).append(" ").append(responseCode).append("\r\n");
        for (HashMap.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        String s = sb.toString();

        byte[] bytes = s.getBytes();
        if (body != null && body.length > 0) {
            bytes = Arrays.copyOf(bytes, bytes.length + body.length);
            System.arraycopy(body, 0, bytes, bytes.length - body.length, body.length);
        }

        return bytes;
    }
}
