
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
    public String toString() {
        //stringbuilder helps us build a string
        StringBuilder sb = new StringBuilder();
        
        sb.append(method + " " + path + " " + httpVersion + "\r\n");
        //header
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey() + ": " + header.getValue() + "\r\n");
        }  
        //end of headers
        sb.append("\r\n");
        //body if one exist
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }

        return sb.toString().trim();
    }
    public String getMethod() {
        return method;

    }
    public String getPath() {
        return path;
    }

}


