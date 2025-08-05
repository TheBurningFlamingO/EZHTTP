package Tools;
import Data.ResponseCode;
import Data.MIMEType;
import Messages.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.nio.file.Files;
import java.io.*;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseBuilder {
    //The only supported HTTP version is 1.1
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String WEB_ROOT = "webroot"; //directory for static files
    private static final String UPLOAD_ROOT = "upload";

    /**
     * A static or utility class may not be instantiated
     */
    private ResponseBuilder() {
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
        if (request == null) {
            throw new InvalidRequestException("Attempt to build response with null request");
        }
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

        //begin sanitizing path
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

            //convert to canonical path format (or linux format if you're more familiar with that)
            decodedPath = decodedPath.replace('\\', '/');

            //split path into segments
            String[] segments = decodedPath.split("/");
            LinkedList<String> cleanSegments = new LinkedList<>();

            //process each segment
            for (String segment : segments) {
                //skip empty segments and references to the current directory
                if (segment.isEmpty() || segment.equals(".")) {
                    continue;
                }

                //handle parent directory references
                if ("..".equals(segment)) {
                    if (!cleanSegments.isEmpty()) {
                        cleanSegments.removeLast();
                    }
                    continue;
                }

                //scrub the segment of any surviving potentially dangerous characters
                segment = segment.replaceAll("[^-zA-Z0-9._-]","");
                if (!segment.isEmpty()) {
                    cleanSegments.add(segment);
                }
            }

            //reconstruct the path
            StringBuilder sb = new StringBuilder();
            for (String segment : cleanSegments) {
                sb.append('/');
                sb.append(segment);
            }
            
            //ensure path begins with '/'
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
     * Handles HTTP GET requests by processing the requested resource path, validating its existence,
     * and building an appropriate response with the file content or an error message.
     *
     * @param request the incoming HTTP GET request containing the request path and other relevant data
     * @return a Response object representing the server's response, including the response code,
     *         headers, and body content based on the requested resource or an error
     * @throws InvalidRequestException if the provided request is null
     */
    private static Response handleGetRequest(Request request) throws InvalidRequestException {
        //a null request implies a critical server error
        if (request == null) {
            throw new InvalidRequestException("Attempt to handle null request");
        }

        //sanitize path
        String path = sanitizePath(request.getPath());

        //Serve index.html for the root path.
        if (path.equals("/")) {
            path = "/index.html";
        }

        //verify requested resources exist on system
        File file = new File(WEB_ROOT + path);

        //read file contents
        try {
            ResponseCode rc = serveFile(file);

            //if error, build error response
            if (rc.isError())
                return buildErrorResponse(rc, request);


            String content = Files.readString(file.toPath());
            HashMap<String, String> headers = new HashMap<>();

            //get MIME type
            MIMEType mimeType = MIMEType.fromString(path);
            headers.put("Content-Type", mimeType.toString());
            headers.put("Content-Length", String.valueOf(content.length()));
            return constructResponse(request, rc, headers, content);
        }
        catch (InvalidRequestException | IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
        catch (Exception e) {
            System.err.println("Unexpected exception in handleGetRequest: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    /**
     * Serves a file by verifying its existence, readability, and returning an appropriate
     * {@link ResponseCode} based on the file's status.
     *
     * @param file the file to be served; it must be a valid file object
     * @return {@link ResponseCode#OK} if the file exists and is readable,
     *         {@link ResponseCode#NOT_FOUND} if the file does not exist or is not a valid file,
     *         {@link ResponseCode#FORBIDDEN} if the file exists but is not readable
     */
    private static ResponseCode serveFile(File file) throws IOException{
        //validate params
        if (file == null)
            throw new IOException("Attempt to serve null file");

        if (!file.exists() || !file.isFile())
            return ResponseCode.NOT_FOUND;
        if (!file.canRead())
            return ResponseCode.FORBIDDEN;

        // security solutions to go here?

        return ResponseCode.OK;
    }

    /**
     * Handles HTTP POST requests by creating a simple response indicating the request was received.
     * This is a placeholder implementation that only acknowledges receipt of the request.
     *
     * @param request the incoming HTTP POST request to be processed
     * @return a Response object representing the server's response to the POST request
     */
    private static Response handlePostRequest(Request request) throws InvalidRequestException {
        //validate request
        if (request == null)
            throw new InvalidRequestException("Attempt to handle null request");

        if (request.getBody() == null || request.getBody().isEmpty()) {
            return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
        }

        try {
            MIMEType contentType = MIMEType.fromString(request.getHeaders().getOrDefault("Content-Type", ""));
            String path = sanitizePath(request.getPath());
            //handle different endpoints
            return switch (path) {
                case "/api/data" -> handleDataPost(request, contentType);
                case "/api/upload" -> handleFileUpload(request, contentType.toString());
                default -> buildErrorResponse(ResponseCode.NOT_FOUND, request);
            };
        }
        catch (IllegalArgumentException e) {
            System.err.println("Invalid content type: " + e.getMessage());
            return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
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
    private static Response handleDataPost(Request request, MIMEType contentType) {
        //data
        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", MIMEType.APP_JSON.toString());

        try {

            //process request body
            String requestBody = request.getBody();

            switch (contentType) {
                case APP_JS -> {

                    //json processing logic here

                    String responseBody = "{\"status\": \"success\"}";
                    responseHeaders.put("Content-Length", String.valueOf(responseBody.length()));
                    return constructResponse(request, ResponseCode.OK, responseHeaders, responseBody);
                }
                case APP_X_WWW_FORM_URLENCODED -> {
                    //Process form data
                    responseHeaders = parseFormData(requestBody);
                    //build response
                    String responseBody = "{\"status\": \"success\"}";
                    responseHeaders.put("Content-Length", String.valueOf(responseBody.length()));
                    return constructResponse(request, ResponseCode.OK, responseHeaders, responseBody);
                }
                case MP_FORM_DATA -> {
                    //process multipart form data
                    responseHeaders = parseMultipartFormData(request);

                    String responseBody = "{\"status\": \"success\"}";
                    responseHeaders.put("Content-Length", String.valueOf(responseBody.length()));
                    return constructResponse(request, ResponseCode.OK, responseHeaders, responseBody);
                }
                default -> {
                    return buildErrorResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing data POST: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }


    /**
     * Handles the upload of files sent in a multipart/form-data HTTP request.
     * Validates the content type, parses the request body to extract files,
     * and saves the files to the server. Returns an appropriate HTTP response
     * based on the result of the file upload operation.
     *
     * @param request the HTTP request containing the headers and body with the uploaded files
     * @param contentType the Content-Type of the request, which must indicate multipart/form-data
     * @return a Response object representing the outcome of the file upload operation,
     *         including the appropriate HTTP status code and any necessary headers or body content
     */
    private static Response handleFileUpload(Request request, String contentType) {
        if (!contentType.startsWith(MIMEType.MP_FORM_DATA.toString())) {
            return buildErrorResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
        }
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", MIMEType.APP_JSON.toString());
        ResponseCode status = ResponseCode.OK;

        try {
            HashMap<String, String> files = parseMultipartFormData(request);
            if (files.isEmpty()) {
                return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
            }

            //process each file
            for (HashMap.Entry<String, String> entry : files.entrySet()) {
                //get field and file data
                String fieldName = entry.getKey().trim();
                String fileData = entry.getValue().trim();

                //validate field name
                if (fieldName.isEmpty()) {
                    return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
                }

                try {
                    //convert string to bytes
                    byte[] fileBytes = Base64.getDecoder().decode(fileData);
                    String fileName = sanitizePath(fieldName);
                    File file = new File(UPLOAD_ROOT + fileName);

                    //validate file path
                    status = getPOSTFileStatus(file);

                    if (status.isError()) {
                        return buildErrorResponse(status, request);
                    }

                    Files.write(file.toPath(), fileBytes);
                }
                catch (IllegalArgumentException e) {
                    return buildErrorResponse(ResponseCode.BAD_REQUEST, request);
                }
                catch (SecurityException e) {
                    return buildErrorResponse(ResponseCode.FORBIDDEN, request);
                }
                catch (IOException e) {
                    return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
                }
            }
            return constructResponse(request, status, headers, "{\"status\": \"success\"}");
        }
        catch (Exception e) {
            System.err.println("Error processing file upload: " + e.getMessage());
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    /**
     * Verifies the status of a given file for a POST operation. Determines whether the file
     * already exists, whether the parent directory is writable, or if the file can be created.
     *
     * @param file the file to check; it represents the target file for the POST operation
     * @return {@link ResponseCode#CONFLICT} if the file already exists,
     *         {@link ResponseCode#FORBIDDEN} if the parent directory is not writable,
     *         or {@link ResponseCode#CREATED} if the file can be created
     */
    private static ResponseCode getPOSTFileStatus(File file) {
        if (file.exists())
            return ResponseCode.CONFLICT;
        if (!file.getParentFile().canWrite())
            return ResponseCode.FORBIDDEN;

        return ResponseCode.CREATED;
    }

    private static HashMap<String, String> processRequestBody(String body) throws Exception {
        HashMap<String, String> data = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return data;
        }
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

        return data;
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

    /**
     * Parses the body of an incoming HTTP request with "multipart/form-data" content type
     * and extracts files, mapping the field names to their data as byte arrays.
     *
     * @param request the HTTP request containing headers and body, expected to have a
     *                "multipart/form-data" content type
     * @return a HashMap where the keys are field names and values are byte arrays representing
     *         the content of the files associated with those field names
     * @throws IllegalArgumentException if the request has an invalid or missing content type,
     *                                  or if the boundary is not correctly specified
     */
    private static HashMap<String, String> parseMultipartFormData(Request request) throws IllegalArgumentException {
        final String DELIMITER = "--";

        //this maps field names to file contents
        HashMap<String, String> files = new HashMap<>();

        if (request == null || request.getBody() == null) {
            return files;
        }

        String contentType = request.getHeaders().get("Content-Type");

        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Invalid multipart form data: invalid content type!");
        }

        String boundary = getBoundary(contentType);
        if (boundary == null) {
            throw new IllegalArgumentException("Invalid multipart form data: no boundary!");
        }

        //build delimiter
        String delimiter = DELIMITER + boundary;
        String closeDelimiter = delimiter + DELIMITER;

        String bodyString = request.getBody();
        String[] parts = bodyString.split(Pattern.quote(delimiter));


        for (String part : parts) {
            part = part.trim();
            //discard empty entries and the final delimiter
            if (part.isEmpty() || part.equals(DELIMITER))
                continue;
            String[] sections = part.split("\r\n\r\n", 2);

            //skip non-partitioned entries
            if (sections.length != 2) {
                continue;
            }

            String headerSection = sections[0].trim();
            String dataSection = sections[1].trim();

            if (headerSection.contains(closeDelimiter)) break;

            Matcher dispositionMatcher = Pattern.compile("Content-Disposition:\\s*form-data;\\s*name=\"([^\"]+)\"(?:;\\s*filename=\"([^\"]+)\")?",
                    Pattern.CASE_INSENSITIVE).matcher(headerSection);

            //not a form data part
            if (!dispositionMatcher.find()) {
                continue;
            }

            String fieldName = dispositionMatcher.group(1);
            String filename = dispositionMatcher.group(2);

            //treat filenames as file uploads
            if (filename != null && !filename.isEmpty()) {
                //strip trailing boundary
                if (dataSection.endsWith(DELIMITER)) {
                    dataSection = dataSection.substring(0, dataSection.length() - DELIMITER.length());
                }
                //convert to bytes
                byte[] fileData = dataSection.getBytes(StandardCharsets.ISO_8859_1);
                String encodedData = Base64.getEncoder().encodeToString(fileData);
                files.put(fieldName, encodedData);

            }
        }
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
        if (boundaryParts[1].endsWith("\n"))
            boundaryParts[1] = boundaryParts[1].substring(0, boundaryParts[1].length() - 1);
        return boundaryParts[1];
    }

    /**
     * Builds an HTTP response using the provided request, response code, headers, and body content.
     * Validates the input arguments and ensures the "Content-Length" header is properly set based on the body content.
     * If any input arguments are invalid, an error response is returned.
     *
     * @param req the incoming HTTP Request object that contains the details of the client's request
     * @param code the HTTP ResponseCode representing the status code and reason phrase for the response
     * @param headers a map of response headers to be included in the response
     * @param body the body of the response as a String
     * @return a Response object representing the constructed HTTP response, or an error response if inputs are invalid
     */
    private static Response constructResponse(Request req, ResponseCode code, HashMap<String, String> headers, String body) {
        //validate arguments
        if (req == null || code == null || headers == null || body == null) {
            //this shouldn't happen; a critical system error
            return buildErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, req);
        }
        //ensure content length is present
        if (!body.isEmpty() && !headers.getOrDefault("Content-Length", "").isEmpty()) {
            headers.put("Content-Length", String.valueOf(body.length()));
        }
        //construct response
        return new Response(HTTP_VERSION, code.toString(), headers, body, req);
    }

    /**
     * Generates an HTTP error response based on the provided response code and request.
     *
     * @param code the ResponseCode object containing the HTTP status code and message for the error response
     * @param req  the incoming Request object used to retrieve the socket for the response
     * @return a Response object representing the constructed HTTP error response
     * @throws InvalidRequestException if the provided request is null
     */
    private static Response buildErrorResponse(ResponseCode code, Request req) throws InvalidRequestException {

        //A null request implies a critical bug in the server backend
        if (req == null) {
            throw new InvalidRequestException("Attempt to build error response with null request");
        }

        HashMap<String, String> headers = new HashMap<>();
        String message = code.getMessage();
        headers.put("Content-Type", "text/plain");
        headers.put("Content-Length", String.valueOf(message.length()));

        return constructResponse(req, code, headers, message);
    }

    /**
     * Thrown to indicate that a request is invalid or does not meet the required criteria.
     * This exception extends {@link IllegalArgumentException} and is used to signify issues
     * such as invalid parameters or malformed requests.
     */
    private static class InvalidRequestException extends IllegalArgumentException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}