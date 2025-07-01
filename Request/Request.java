
package Request;
import java.net.Socket;
import java.util.HashMap;
import java.io.*;
public class Request implements IRequest {
    private String method = "";
    private String path = "";
    private String httpVersion;
    private HashMap<String, String> headers = new HashMap<>();
    private String body = "";



    public Request(String method, String path, String httpVersion, HashMap<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.body = body;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(path).append(" ").append(httpVersion).append("\r\n");
        //append headers
        for (HashMap.Entry<String, String> entry: headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");

        }
        //separate headers from body
        sb.append("\r\n");
        //append body if it exists
        if (!body.isEmpty()) {
            sb.append(body);
        }
        return sb.toString();
    }
    public String getMethod() {
        return method;
    }
    public String getPath() {
        return path;
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
}
