package Messages;
import java.util.HashMap;
import java.net.Socket;
public abstract class Message {
    protected String httpVersion;
    protected HashMap<String, String> headers;
    protected String body;
    protected Socket socket;

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
}