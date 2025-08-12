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
 * The {@code RequestParser} class is a utility for parsing HTTP requests from client sockets.
 * It provides methods to parse raw HTTP data into a structured {@code Request} object, validate
 * the request, and extract form data or multipart data from its body.
 *
 * This class is designed to support only certain HTTP request methods as specified
 * in its internal configuration. It is not intended to be instantiated, as all its methods
 * are static.
 */
public class RequestParser {
    //the parser only accepts methods specified in this array
    private static final Set<String> ACCEPTED_METHODS = Set.of(
        "GET",
        "POST"
    );

    /**
     * Constructor for the {@code RequestParser} class.
     *
     * This constructor is private to enforce that this class acts as a utility
     * class and should not be instantiated. Attempting to create an instance
     * of this class will result in an {@code UnsupportedOperationException}.
     *
     * This class provides static methods to parse, validate, and
     * process HTTP requests.
     *
     * @throws UnsupportedOperationException always thrown to prevent instantiation
     */
    public RequestParser() {
        throw new UnsupportedOperationException("A utility class should not be instantiated!");
    }


    /**
     * Parses an incoming HTTP request from the provided server socket and constructs a {@code Request} object.
     *
     * This method reads and extracts the HTTP method, the requested path, version, headers, and body (if present)
     * from the socket input stream. The resulting {@code Request} object is validated for correctness before being returned.
     *
     * @param serverSocket the {@code Socket} object representing the client-server connection from which the HTTP request is read
     * @return a {@code Request} object containing the parsed HTTP request details
     * @throws IOException if an I/O error occurs while reading from the socket
     * @throws IllegalStateException if the parsed HTTP request is found to be invalid
     */
    public static Request parse(Socket serverSocket) throws IOException, IllegalStateException {
        //prepare to read from the socket
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
        //start with request line
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
     * Validates whether the given HTTP request uses an accepted HTTP method.
     *
     * The method checks if the provided {@code Request} object is not null and verifies
     * that its HTTP method matches one of the methods allowed in the {@code ACCEPTED_METHODS} list.
     *
     * @param req the {@code Request} object to validate
     * @return {@code true} if the request is valid and uses an accepted HTTP method,
     *         {@code false} otherwise
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

    /**
     * Parses the body of a POST request with URL-encoded form data into a map of key-value pairs.
     *
     * This method validates that the provided request uses the HTTP POST method
     * and has a "Content-Type" of "application/x-www-form-urlencoded". If these conditions
     * are not met, an {@link IllegalArgumentException} will be thrown. If the request body
     * is empty or null, an empty map will be returned.
     *
     * @param request the HTTP {@link Request} object containing the form data to be parsed
     * @return a {@link HashMap} containing key-value pairs parsed from the form data
     * @throws IllegalArgumentException if the request is not a POST request or
     *         does not have "application/x-www-form-urlencoded" as the Content-Type header
     */
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

    /**
     * Parses the body of a multipart form POST request into a map of key-value pairs
     * where the keys are strings and the values are byte arrays.
     *
     * This method validates that the provided request uses the HTTP POST method and
     * has a "Content-Type" header specifying multipart form data. If these conditions
     * are not met, an {@link IllegalArgumentException} will be thrown. If the request
     * body or headers are invalid, an empty map will be returned or an exception may
     * be thrown based on the condition.
     *
     * @param request the HTTP {@link Request} object to be processed
     * @return a {@link HashMap} containing key-value pairs where keys are strings
     *         and values are byte arrays extracted from the multipart form data
     * @throws IllegalArgumentException if the request is not a POST request or
     *         does not have "multipart/form-data" as the Content-Type header
     */
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
     */
    class FormDataParser {

        /**
         * Parses a URL-encoded string into a HashMap where keys and values represent
         * the decoded key-value pairs from the input string.
         *
         * @param body the URL-encoded string containing key-value pairs separated by '&'
         *             and key-value pairs separated by '='. This can typically be extracted from
         *             the body of an HTTP POST request.
         * @return a HashMap containing the decoded form data, with each key mapped to its corresponding value.
         *         If the input string is null or empty, an empty HashMap is returned.
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