
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



    /**
     * Reads an incoming request from a bound socket
     * Precondition: Request is active and non-empty
     * Postcondition: an object containing the message text and its size
     * @param socket (Socket) - The socket to read the message from
     */
    public Request(Socket socket) {
        try {
            //read input from socket
            InputStream is = socket.getInputStream();
            InputStreamReader isReader = new InputStreamReader(is);
            BufferedReader isBuf = new BufferedReader(isReader);
            
            //read the request line
            String line = isBuf.readLine();
            if (line != null && !line.isEmpty()) {
                //split the line up into its words
                String[] parts = line.split(" ");
                //the request line has 3 fields
                if (parts.length == 3) {
                    method = parts[0];
                    path = parts[1];
                    httpVersion = parts[2];
                }
            }
            while ((line = isBuf.readLine()) != null && !line.isEmpty()) {
                int colonIndex = line.indexOf(":");
                if (colonIndex != -1) {
                    String key = line.substring(0, colonIndex).trim();
                    String value = line.substring(colonIndex + 1).trim();
                    headers.put(key, value);
                }
            }

            //read the body if one exists
            if (headers.containsKey("Content-Length")) {
                int length = Integer.parseInt(headers.get("Content-Length"));
                char[] bodyChars = new char[length];
                int read = isBuf.read(bodyChars);
                if (read > 0) {
                    body = new String(bodyChars, 0, read);
                }
            }

            //free readers
            isBuf.close();
            isReader.close();
            is.close();
        }
        catch (IOException e) {
            System.out.println("Error receiving input stream");
            System.out.println(e.getMessage());
        }
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
