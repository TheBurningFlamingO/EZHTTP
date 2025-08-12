package Messages;
import Data.ResponseCode;
import Tools.TxnLogger;

import java.util.*;
import java.net.Socket;
public class Response extends Message {
    //the response code
    private final String responseCode;

    private boolean isBinary = false;
    private byte[] binaryBody;


    public Response(Request request, String httpVersion, ResponseCode rc, HashMap<String, String> headers, byte[] binaryBody) {
        super(httpVersion, headers, "", request.getSocket());
        this.responseCode = rc.toString();
        this.body = binaryBody;
        this.isBinary = true;

    }

    public byte[] getBinaryBody() {
        return binaryBody;
    }
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
