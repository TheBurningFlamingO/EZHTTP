package Tools;
import Data.ResponseCode;
import Messages.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.nio.file.Files;
import java.io.*;
import java.util.LinkedList;


public class ResponseBuilder {
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String WEB_ROOT = "webroot"; //directory for static files

    public ResponseBuilder() {
        throw new IllegalStateException("Attempt to instantiate static class");

    }


    /**
     * Builds an HTTP response based on the method of the incoming request.
     * Supports handling of GET and POST requests, returning appropriate responses
     * depending on the method, and generates an error response for unsupported methods.
     *
     * @param request the incoming HTTP request containing method, headers, and other data
     * @return a Response object representing the server's response to the specified request,
     *         including the response code, headers, body, and other relevant information
     */
    public static Response buildResponse(Request request) {
        return switch (request.getMethod()) {
            case "GET" -> handleGetRequest(request);
            case "POST" -> handlePostRequest(request);
            default -> buildErrorResponse(ResponseCode.METHOD_NOT_ALLOWED, request);
        };
    }


    /**
     * Sanitizes a given file path by decoding URL-encoded characters, removing query parameters
     * and fragments, resolving relative path references (e.g., "." and ".."), and normalizing
     * the path structure to a canonical form.
     *
     * @param path the raw input path to sanitize; could be null, empty, or contain invalid or dangerous characters
     * @return a sanitized and canonicalized path string, starting with "/", or "/" as a default
     *         in case of invalid or empty input
     */
    private static String sanitizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        try {
            //decode URL encoded characters
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);

            //remove query parameters and fragments
            int questionMark = decodedPath.indexOf('?');
            if (questionMark != -1) {
                decodedPath = decodedPath.substring(0, questionMark);
            }

            int hashMark = decodedPath.indexOf('#');
            if (hashMark != -1) {
                decodedPath = decodedPath.substring(0, hashMark);
            }

            //convert to canonical path format
            decodedPath = decodedPath.replace('\\', '/');

            //split path into segments
            String[] segments = decodedPath.split("/");
            LinkedList<String> cleanSegments = new LinkedList<>();

            //Process each segment
            for (String segment : segments) {
                //skip empty segments and references to the current directory
                if (segment.isEmpty() || segment.equals(".")) {
                    continue;
                }

                //Handle parent directory references
                if ("..".equals(segment)) {
                    if (!cleanSegments.isEmpty()) {
                        cleanSegments.removeLast();
                    }
                    continue;
                }

                //remove any potentially dangerous characters
                segment = segment.replaceAll("[^-zA-Z0-9._-]","");
                if (!segment.isEmpty()) {
                    cleanSegments.add(segment);
                }
            }

            //Reconstruct the path
            StringBuilder sb = new StringBuilder();
            for (String segment : cleanSegments) {
                sb.append('/');
                sb.append(segment);
            }
            
            // Ensure path begins with '/'
            if (sb.isEmpty())
                return "/";
            
            return sb.toString();
        }
        catch (Exception e) {
            //if any exception occurs, return the root path
            return "/";
        }
    }

    /**
     * Handles HTTP GET requests by serving requested files from the server's web root directory.
     * If the requested file does not exist, it returns an error response with an appropriate status code.
     *
     * @param request the incoming HTTP GET request to be processed
     * @return a Response object representing the server's response to the GET request, including file content
     *         if found or an error message if the requested file is missing or an internal error occurs
     */
    private static Response handleGetRequest(Request request) {
        String path = sanitizePath(request.getPath());
        ResponseCode rc = ResponseCode.OK;

        //Sanitize the path to prevent a directory traversal attack
        path = path.replaceAll("\\.\\.", "");

        //Serve index.html for the root path.
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(WEB_ROOT + path);
        if (!file.exists()) {
            rc = ResponseCode.NOT_FOUND;
            return buildErrorResponse(rc, request);
        }

        //read file contents
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            //set up headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", getMimeType(path));
            headers.put("Content-Length", String.valueOf(content.length));
            return new Response(HTTP_VERSION, rc.toString(), headers, new String(content), request);

        }
        catch (IOException e) {
            rc = ResponseCode.INTERNAL_SERVER_ERROR;
            return buildErrorResponse(rc, request);
        }
    }

    /**
     * Handles HTTP POST requests by creating a simple response indicating the request was received.
     * This is a placeholder implementation that only acknowledges receipt of the request.
     *
     * @param request the incoming HTTP POST request to be processed
     * @return a Response object representing the server's response to the POST request
     */
    private static Response handlePostRequest(Request request) {
        // POST request handling logic to be added
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        //acknowledge receipt of request for now
        String responseBody = "{\"status\": \"received\"";
        headers.put("Content-Length", String.valueOf(responseBody.length()));

        return new Response(HTTP_VERSION, ResponseCode.OK.toString(), headers, responseBody, request);


    }

    /**
     * Determines the MIME (Multipurpose Internet Mail Extension) type for a given file based on its path or extension.
     *
     * @param path the file path or name whose MIME type needs to be identified
     * @return the MIME type as a string, such as "text/html", "application/javascript",
     *         "image/png", etc. Defaults to "text/plain" for unrecognized file types
     */
    private static String getMimeType(String path) {
        if (path.endsWith(".html"))
            return "text/html";
        if (path.endsWith(".css"))
            return "text/css";
        if (path.endsWith(".js"))
            return "application/javascript";
        if (path.endsWith(".png"))
            return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        return "text/plain";
    }

    /**
     * Generates an HTTP error response based on the provided response code and request.
     *
     * @param code the ResponseCode object containing the HTTP status code and message for the error response
     * @param req the incoming Request object used to retrieve the socket for the response
     * @return a Response object representing the constructed HTTP error response
     */
    private static Response buildErrorResponse(ResponseCode code, Request req) {
        HashMap<String, String> headers = new HashMap<>();
        String message = code.getMessage();
        headers.put("Content-Type", "text/plain");
        headers.put("Content-Length", String.valueOf(message.length()));

        return new Response(HTTP_VERSION, code.toString(), headers, message, req);
    }
}
