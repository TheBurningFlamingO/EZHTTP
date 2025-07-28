package Messages;
import java.util.HashMap;
import java.net.Socket;
import Tools.TxnLogger;
public abstract class Message {
    protected String httpVersion;
    protected HashMap<String, String> headers;
    protected String body;
    protected Socket socket;
    protected TxnLogger txnLogger;

    public Message() {
        httpVersion = "";
        headers = new HashMap<>();
        body = "";
        socket = null;
    }
    
    public Message(String httpVersion, HashMap<String, String> headers, String body, Socket socket) {
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.body = body;
        this.socket = socket;
    }

    public String getHTTPVersion() {
        return httpVersion;
    }
    public HashMap<String, String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }
    public Socket getSocket() { 
        return socket;
    }

    public abstract String toString();

    /**
     * Appends the message tail by constructing a string representation of the headers
     * and the body of the message, if present. The headers are formatted as
     * "key: value" pairs separated by a line break, followed by a blank line. If
     * the body is not null or empty, it is appended after the headers.
     *
     * @return a string representation of the message tail, including headers and the body if any
     */
    protected String appendMessageTail() {
        StringBuilder sb = new StringBuilder();
        for (HashMap.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }

        //whitespace at end of headers
        sb.append("\r\n");

        //append body if one exists
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }
        return sb.toString().trim();
    }
}