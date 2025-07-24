package Messages;
import java.util.*;
import java.net.Socket;
public class Response extends Message {
    private final String responseCode;
    public Response(String httpVersion, String responseCode, HashMap<String, String> headers, String body,
    Socket socket) {
        super(httpVersion, headers, body, socket);
        this.responseCode = responseCode;
    }
    
    public String toString() {
        String s = httpVersion + " " + responseCode + "\r\n" +
                //get the message tail (same for all messages)
                appendMessageTail();

        return s.trim();
    }
    public String getResponseCode() {
        return responseCode;
    }
}
