package Tools;
import Data.ResponseCode;
import Messages.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.nio.file.Files;
import java.io.*;
import java.util.LinkedList;
import java.util.Map;


public class ResponseBuilder {
    //The only supported HTTP version is 1.1
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String WEB_ROOT = "webroot"; //directory for static files

    /**
     * A static or utility class may not be instantiated
     */
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
        //Return error response if request is null
        //a null request implies a critical server error
        if (request == null) {
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }

        //sanitize path
        String path = sanitizePath(request.getPath());
        ResponseCode rc = ResponseCode.OK;

        //Serve index.html for the root path.
        if (path.equals("/")) {
            path = "/index.html";
        }

        //verify requested resources exist on system
        File file = new File(WEB_ROOT + path);
        if (!file.exists() || !file.isFile()) {
            rc = ResponseCode.NOT_FOUND;
            return buildErrorResponse(rc, request);
        }

        //check if file is readable
        if (!file.canRead()) {
            rc = ResponseCode.FORBIDDEN;
            return buildErrorResponse(rc, request);
        }

        //read file contents
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            //set up headers
            HashMap<String, String> headers = new HashMap<>();
            String mimeType = getMimeType(path);
            headers.put("Content-Type", mimeType);
            headers.put("Content-Length", String.valueOf(content.length));
            String body = new String(content, StandardCharsets.UTF_8);
            return new Response(HTTP_VERSION, rc.toString(), headers, body, request);

        }
        catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
        catch (Exception e) {
            System.err.println("Unexpected exception in handleGetRequest: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
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
        //validate request
        if (request == null || request.getBody() == null) {
            return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
        }

        //get and validate content type
        String contentType = request.getHeaders().getOrDefault("Content-Type", "");
        if (contentType.isEmpty()) {
            return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
        }

        try {
            String path = sanitizePath(request.getPath());
            //handle different endpoints
            return switch (path) {
                case "/api/data" -> handleDataPost(request, contentType);
                case "/api/upload" -> handleFileUpload(request, contentType);
                default -> buildErrorResponse(ResponseCode.NOT_FOUND, request);
            };
        }
        catch (Exception e) {
            System.err.println("Error handling POST request: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }

    }

    /**
     * Handles HTTP POST requests by parsing the request body, processing data based on the specified content type,
     * and generating an appropriate HTTP response.
     *
     * @param request the incoming HTTP POST request containing headers, body, and other data
     * @param contentType the content type of the request body, used to determine how the data should be processed
     * @return a Response object representing the server's response to the POST request,
     *         including the appropriate status code, headers, and body content based on the processing results
     */
    private static Response handleDataPost(Request request, String contentType) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {

            //process request body
            String requestBody = request.getBody();
            HashMap<String, String> data = new HashMap<>();
            processRequestBody(requestBody, data);

            switch (contentType.toLowerCase()) {
                case "application/json" -> {

                    //json processing logic here

                    String responseBody = "{\"status\": \"success\"}";
                    headers.put("Content-Length", String.valueOf(responseBody.length()));
                    return new Response(HTTP_VERSION, ResponseCode.OK.toString(), headers, responseBody, request);
                }
                case "application/x-www-form-urlencoded" -> {
                    //Process form data
                    headers = parseFormData(requestBody);
                    //build response
                    String responseBody = "{\"status\": \"success\"}";
                    headers.put("Content-Length", String.valueOf(responseBody.length()));
                    return new Response(HTTP_VERSION, ResponseCode.OK.toString(), headers, responseBody, request);
                }
                case "multipart/form-data" -> {
                    //process multipart form data
                    //headers = parseMultipartFormData(request); --not yet fully implemented

                    String responseBody = "{\"status\": \"success\"}";
                    headers.put("Content-Length", String.valueOf(responseBody.length()));
                    return new Response(HTTP_VERSION, ResponseCode.OK.toString(), headers, responseBody, request);
                }
                default -> {
                    return buildErrorResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error processing data POST: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }


    private static Response handleFileUpload(Request request, String contentType) {
        if (!contentType.startsWith("multipart/form-data")) {
            return buildErrorResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
        }
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        try {
            //process file upload
            String requestBody = request.getBody();
            HashMap<String, String> data = new HashMap<>();
            processRequestBody(requestBody, data);

            String responseBody = "{\"status\": \"success\", \"message\": \"File uploaded successfully\"}";
            data.put("Content-Length", String.valueOf(responseBody.length()));
            return new Response(HTTP_VERSION, ResponseCode.OK.toString(), headers, responseBody, request);

        }
        catch (Exception e) {
            System.err.println("Error processing file upload: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    private static void processRequestBody(String body, HashMap<String, String> data) throws Exception {
        String[] keyValuePairs = body.split("&");
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                data.put(keyValue[0], keyValue[1]);
            }
            else {
                throw new Exception("Invalid key-value pair in request body");
            }
        }
    }

    private static HashMap<String, String> parseFormData(String body) {
        HashMap<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return params;
        }

        /*
         * -TODO: Refactor for efficiency and security
         */
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                try {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                }
                catch (Exception e) {
                    System.err.println("Error parsing form data: " + e.getMessage());
                }
            }
        }
        return params;
    }

    private static HashMap<String, Byte[]> parseMultipartFormData(Request request) {
        HashMap<String, Byte[]> files = new HashMap<>();
        if (request == null || request.getBody() == null) {
            return files;
        }
        String boundary = getBoundary(request.getHeaders().get("Content-Type"));
        /*
         * -TODO: implement multipart form data processing
         */

        return files;
    }

    private static String getBoundary(String contentType) {
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            return null;
        }
        String[] boundaryParts = contentType.split("boundary=");
        if (boundaryParts.length != 2) {
            return null;
        }
        return boundaryParts[1];
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
        //A null request implies a critical bug in the server backend
        if (req == null) {
            //Create a minimal error response if the request is null
            HashMap<String, String> headers = new HashMap<>();
            String message = "Internal Server Error: Invalid Request";
            headers.put("Content-Type", "text/plain");
            headers.put("Content-Length", String.valueOf(message.length()));
            if (code != ResponseCode.INTERNAL_SERVER_ERROR)
                code = ResponseCode.INTERNAL_SERVER_ERROR;
            return new Response(HTTP_VERSION, code.toString(), headers, message, req);
        }

        HashMap<String, String> headers = new HashMap<>();
        String message = code.getMessage();
        headers.put("Content-Type", "text/plain");
        headers.put("Content-Length", String.valueOf(message.length()));

        return new Response(HTTP_VERSION, code.toString(), headers, message, req);
    }
}
