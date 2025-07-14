package Tools;
import Messages.Request;
import java.util.HashMap;
import java.io.*;
import java.net.Socket;
/**
 * Tool class; static methods only
 */
public class RequestParser {
    private static final String[] ACCEPTED_METHODS = {
        "GET",
        "POST"
    };

    public RequestParser() {
        throw new UnsupportedOperationException("Utility class!");
    }
    public static Request parse(Socket serverSocket) throws IOException {
        InputStream is = serverSocket.getInputStream();
        InputStreamReader isReader = new InputStreamReader(is);
        BufferedReader bReader = new BufferedReader(isReader);

        String method = "",
               path = "",
               httpVersion = "",
               body = "";
        HashMap<String, String> headers = new HashMap<>();

        String line = bReader.readLine();
        //read request contents
        if (line != null && !line.isEmpty()) {
            String[] parts = line.split(" ");
            if (parts.length == 3) {
                method = parts[0];
                path = parts[1];
                httpVersion = parts[2];
            }
        }

        // parse the headers
        while ((line = bReader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(":");
            String key = line.substring(0, colonIndex).trim();
            String value = line.substring(colonIndex + 1).trim();
            headers.put(key, value);
        }
        //read body of message if one exists
        if (headers.containsKey("Content-Length")) {
            int length = Integer.parseInt(headers.get("Content-Length"));
            char[] bodyChars = new char[length];
            int read = bReader.read(bodyChars);
            if (read > 0) {
                body = new String(bodyChars, 0, read);
            }
        }
        Request result = new Request(method, path, httpVersion, headers, body, serverSocket);
        if (validate(result)) {
            return result;
        }

        throw new IllegalStateException("Request invalid!");
    }

    public static boolean validate(Request req) {
        for (String method : ACCEPTED_METHODS) {
            if (method.equals(req.getMethod()))
                return true;
        }

        return false;
    }
}