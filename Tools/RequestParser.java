package Tools;
import Messages.Request;
import java.util.HashMap;
import java.io.*;
import java.net.Socket;

/**
 * The {@code RequestParser} class provides functionality for parsing HTTP requests
 * received via a server socket. It is designed to extract information such as
 * the request method, path, HTTP version, headers, and body from input streams
 * associated with client connections. The parsed data is encapsulated in a
 * {@code Request} object.
 *
 * The class incorporates basic validation of HTTP methods using pre-defined
 * accepted methods, which include "GET" and "POST". It also enforces a utility
 * class design by preventing instantiation.
 */
public class RequestParser {
    //the parser only accepts methods specified in this array
    private static final String[] ACCEPTED_METHODS = {
        "GET",
        "POST"
    };

    /**
     * Constructs a new instance of the {@code RequestParser} class.
     *
     * This constructor is private to prevent instantiation of the utility class,
     * as the {@code RequestParser} class operates exclusively with static methods.
     *
     * @throws UnsupportedOperationException when attempting to instantiate the class
     */
    public RequestParser() {
        throw new UnsupportedOperationException("A utility class should not be instantiated!");
    }


    /**
     * Parses an incoming request from a client socket and constructs a {@code Request} object
     * representing the HTTP request.
     *
     * This method reads data from the specified client {@code Socket}, parses the HTTP request
     * line, headers, and body (if present), and validates the resulting {@code Request} object.
     * It throws exceptions if an error occurs during parsing or if the request is found invalid.
     *
     * @param serverSocket the client {@code Socket} from which the HTTP request data will be read
     * @return a {@code Request} object representing the parsed HTTP request, including headers,
     *         path, method, HTTP version, and body
     * @throws IOException if an I/O error occurs while reading from the socket
     * @throws IllegalStateException if the constructed {@code Request} is invalid
     */
    public static Request parse(Socket serverSocket) throws IOException, IllegalStateException {
        InputStream is = serverSocket.getInputStream();
        InputStreamReader isReader = new InputStreamReader(is);
        BufferedReader bReader = new BufferedReader(isReader);

        //initialize working variables
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
        //read the message body if one should exist
        if (headers.containsKey("Content-Length")) {
            int length = Integer.parseInt(headers.get("Content-Length"));
            char[] bodyChars = new char[length];
            int read = bReader.read(bodyChars);
            if (read > 0) {
                body = new String(bodyChars, 0, read);
            }
        }

        //construct result
        Request result = new Request(method, path, httpVersion, headers, body, serverSocket);
        if (validate(result)) {
            return result;
        }

        //a valid request should never reach this line
        throw new IllegalStateException("Request invalid!");
    }

    /**
     * Validates the HTTP request method against the list of accepted methods.
     *
     * This method checks whether the HTTP method of the provided {@code Request}
     * object is included in the predefined list of accepted methods. If the method
     * matches any of the accepted methods, it returns {@code true}; otherwise,
     * it returns {@code false}.
     *
     * @param req the {@code Request} object whose HTTP method will be validated
     *            against the accepted methods
     * @return {@code true} if the HTTP method of the given {@code Request} is
     *         valid and accepted; {@code false} otherwise
     */
    public static boolean validate(Request req) {
        if (req == null)
            return false;

        String theMethod = req.getMethod().toLowerCase();
        for (String method : ACCEPTED_METHODS) {
            if (method.toLowerCase().equals(theMethod))
                return true;
        }

        return false;
    }
}