package Tools;
import Messages.Request;
import Data.MIMEType;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.io.*;
import java.net.Socket;
import java.util.Set;

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
    private static final Set<String> ACCEPTED_METHODS = Set.of(
        "GET",
        "POST"
    );

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
               httpVersion = "";

        byte[] body = null;
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
            byte[] bodyBytes = new byte[length];
            int bytesRead = 0;
            while (bytesRead < length) {
                bytesRead += is.read(bodyBytes, bytesRead, length - bytesRead);
            }
            body = bodyBytes;
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

    public static HashMap<String, String> getFormKeyPairs(Request request) throws IllegalArgumentException {
        if (!request.getMethod().equals("POST"))
            throw new IllegalArgumentException("Request must be a POST request!");

        if (request.getBody() == null || request.getBody().length == 0) {
            return new HashMap<>();
        }
        MIMEType contentType = MIMEType.fromHeader(request.getHeaders().getOrDefault("Content-Type", ""));
        if (!contentType.equals(MIMEType.APP_X_WWW_FORM_URLENCODED))
            throw new IllegalArgumentException("Request must be a URL-encoded form request!");

        return FormDataParser.parseUrlEncoded(new String(request.getBody()));
    }

    public static HashMap<String, byte[]> getMultipartKeyPairs(Request request) throws IllegalArgumentException {
        if (!request.getMethod().equals("POST"))
            throw new IllegalArgumentException("Request must be a POST request!");
        if (request.getHeaders().getOrDefault("Content-Type", "").isEmpty()) {
            return new HashMap<>();
        }
        MIMEType contentType = MIMEType.fromHeader(request.getHeaders().get("Content-Type"));
        if (!contentType.equals(MIMEType.MP_FORM_DATA)) {
            throw new IllegalArgumentException("Request must be a multipart form request!");
        }
        return FormDataParser.parseMultipart(request);
    }
}

    /**
     * Processes form data, returning a hash map with key pairs of the fields and the data
     @implNote This should be part of a handler (Reilly)
     */
    class FormDataParser {

        /**
         * Parses a URL-encoded string and converts it into a HashMap containing key-value
         * pairs, where keys and values are decoded using UTF-8 encoding.
         *
         * @param body the URL-encoded string containing key-value pairs, typically in
         *             the format "key1=value1&key2=value2". Can be null or empty.
         * @return a HashMap containing the key-value pairs extracted and decoded from the provided string.
         *         Returns an empty HashMap if the input is null, empty, or if no valid key-value pairs are found.
         */
        public static HashMap<String, String> parseUrlEncoded(String body) {
            HashMap<String, String> formData = new HashMap<>();
            if (body == null || body.isEmpty()) {
                return formData;
            }

            //store key pairs
            Arrays.stream(body.split("&"))
                    .map(pair -> pair.split("="))
                    .filter(pair -> pair.length == 2)
                    .forEach(pair -> {
                        try {
                            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                            String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            formData.put(key, value);
                        } catch (Exception e) {
                            //skip malformed entries
                            System.err.println("Malformed form data entry: " + e.getMessage());
                        }
                    });

            return formData;
        }

        public static HashMap<String, byte[]> parseMultipart(Request request) {
            return MultipartParser.parse(request);
        }
}