package Tools;

import Data.ResponseCode;
import Data.MIMEType;
import Messages.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Response Builder system
 * Processes an HTTP request and constructs an appropriate response
 */
public class RBRefactor {
    //constants - todo make configurable (besides the HTTP version)
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String WEB_ROOT = "webroot";
    private static final String UPLOAD_ROOT = "upload";
    private static final long MAX_FILE_SIZE = 1024 * 1024 * 10;

    private static final Map<String, String> SECURITY_HEADERS = Map.of(
            "X-Content-Type-Options", "nosniff",
            "X-XSS-Protection", "1; mode=block",
            "X-Frame-Options", "DENY",
            "Content-Security-Policy", "default-src 'self'",
            "Strict-Transport-Security", "max-age=31536000; includeSubDomains",
            "Referrer-Policy", "strict-origin-when-cross-origin"
    );

    //forbidden extensions - also todo make configurable
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "php", "asp", "aspx", "jsp", "php3", "php4", "phtml", "exe", "bat", "sh", "dll", "py", "pl", "rb"
    );

    //cannot instantiate utility class
    private RBRefactor() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds an HTTP response based on the provided request. The method validates the
     * request and processes it according to its HTTP method (GET or POST). If the method
     * is unsupported, an error response is constructed.
     *
     * @param request the HTTP request to be processed, containing the information
     *                needed to determine the appropriate response
     * @return a Response object containing the HTTP response, including status code,
     *         headers, and any response body
     */
    public static Response buildResponse(Request request) {
        validateRequest(request);
        return switch (request.getMethod()) {
            case "GET" -> handleGetRequest(request);
            case "POST" -> handlePostRequest(request);
            default -> constructErrorResponse(ResponseCode.METHOD_NOT_ALLOWED, request);
        };
    }

    /**
     * Validates the given HTTP request to ensure it meets the expected criteria.
     * The request is checked using the {@link RequestParser#validate(Request)} method.
     * If the request is invalid, an {@link InvalidRequestException} is thrown.
     *
     * @param request the HTTP request to be validated
     * @throws InvalidRequestException if the request is null or does not pass validation
     */
    private static void validateRequest(Request request) {
        if (!RequestParser.validate(request)) {
            throw new InvalidRequestException("Request cannot be null!");
        }
    }

    /**
     * Handles an HTTP GET request by validating the request, resolving the requested resource path,
     * and constructing an appropriate HTTP response. This method ensures that the requested file
     * exists and is accessible, builds necessary headers, and includes the requested content in
     * the response body if applicable. In case of errors, such as invalid paths or inaccessible
     * files, an error response is constructed and returned.
     *
     * @param request the HTTP GET request to be processed, which includes the requested resource path
     *                and other necessary details for handling.
     * @return a Response object representing the HTTP response, which includes the status code,
     *         headers, and body containing the requested resource or an error message in case of failure.
     */
    private static Response handleGetRequest(Request request) {
        validateRequest(request);
        String path = FileHandler.sanitizePath(request.getPath());
        path = path.equals("/") ? "/index.html" : path;

        File file = new File(WEB_ROOT + path);
        try {
            ResponseCode rc = FileHandler.validateFileAccess(file);
            if (rc.isError()) {
                return constructErrorResponse(rc, request);
            }

            String content = Files.readString(file.toPath());
            HashMap<String, String> headers = buildContentHeaders(path, content);
            return constructResponse(request, rc, headers, content);
        }
        catch (Exception e) {
            logError("Error handling GET request: ", e);
            return constructErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    /**
     * Builds a mapping of HTTP content headers based on the provided file path and content.
     * This method generates headers such as "Content-Type" and "Content-Length" by determining
     * the MIME type from the file extension of the given path and calculating the content's size.
     *
     * @param path the file path used to determine the MIME type
     * @param content the content whose length is used to calculate the "Content-Length" header
     * @return a HashMap containing the generated HTTP headers, including "Content-Type" and "Content-Length"
     */
    private static HashMap<String, String> buildContentHeaders(String path, String content) {
        HashMap<String, String> headers = new HashMap<>();
        MIMEType mimeType = MIMEType.fromFileExtension(path);
        headers.put("Content-Type", mimeType.toString());
        headers.put("Content-Length", String.valueOf(content.length()));
        return headers;
    }

    /**
     * Handles an HTTP POST request by validating the request, determining the
     * appropriate content type, and routing the request to the corresponding
     * handler based on the requested path. Constructs an error response for
     * invalid requests, unsupported paths, or internal server errors.
     *
     * @param request the HTTP POST request to be processed, containing headers, body,
     *                and the requested resource path
     * @return a Response object representing the HTTP response, which includes the
     *         status code, headers, and body with the results of the processing or
     *         an error message if the request fails
     */
    private static Response handlePostRequest(Request request) {
        //ensure request is valid
        validateRequest(request);
        if (isInvalidPostRequest(request))
            return constructErrorResponse(ResponseCode.BAD_REQUEST, request);

        //process request
        try {
            String contentTypeLine = request.getHeaders().getOrDefault("Content-Type", "");

            MIMEType contentType = MIMEType.fromHeader(contentTypeLine);
            String path = FileHandler.sanitizePath(request.getPath());
            System.out.println("POST Request to " + path + " with content type " + contentType);
            return switch (path) {
                case "/api/data" -> handleDataPost(request, contentType);
                case "/api/upload" -> handleFileUpload(request, contentTypeLine);
                case "/api/echo" -> handleEcho(request, contentType);
                default -> constructErrorResponse(ResponseCode.NOT_FOUND, request);
            };
        }
        catch (Exception e) {
            logError("Error handling POST Request", e);
            return constructErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    /**
     * Handles an "echo" operation by generating an HTTP response that mirrors the request body.
     * The response includes appropriate headers for content type and content length.
     *
     * @param request the HTTP request to be processed, containing the body to echo back in the response
     * @param contentType the MIME type to set in the "Content-Type" header of the response
     * @return a Response object containing the HTTP response with a status code of 200 (OK),
     *         headers specifying content details, and the body matching the request's body
     */
    private static Response handleEcho(Request request, MIMEType contentType) {
        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", contentType.toString());
        responseHeaders.put("Content-Length", String.valueOf(request.getBody().length()));

        return constructResponse(request, ResponseCode.OK, responseHeaders, request.getBody());
    }

    /**
     * Determines if the provided HTTP POST request is invalid. A POST request is
     * considered invalid if its body is null or empty.
     *
     * @param request the HTTP request to be validated, containing headers, body,
     *                and metadata about the POST operation
     * @return true if the request is invalid due to a null or empty body, false otherwise
     */
    private static boolean isInvalidPostRequest(Request request) {
        return request.getBody() == null || request.getBody().isEmpty();
    }

    /**
     * Processes an HTTP POST request by determining the specified MIME type of the request's content
     * and routing it to the appropriate handler function. Constructs an HTTP response based on the
     * outcome of the content processing. In cases where the MIME type is unsupported, or if an error
     * occurs during processing, constructs an appropriate error response.
     *
     * @param request the HTTP request to be handled, containing headers, body, and metadata.
     * @param contentType the MIME type of the request body, used to determine the appropriate handler.
     * @return a Response object representing the HTTP response, containing the status code, response
     *         headers, and response body.
     */
    private static Response handleDataPost(Request request, MIMEType contentType) {
        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", MIMEType.APP_JSON.toString());

        try {
            return switch (contentType) {
                case APP_JSON -> handleJsonDataRequest(request, responseHeaders);
                case APP_X_WWW_FORM_URLENCODED -> handleFormData(request, responseHeaders);
                case MP_FORM_DATA -> handleMultipartData(request, responseHeaders);
                default -> constructErrorResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
            };
        }
        catch (Exception e) {
            logError("Error processing data post", e);
            return constructErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    /**
     * Handles form data in an HTTP POST request by generating an appropriate response.
     * This method processes the incoming request, builds necessary content headers,
     * and constructs a response object with a success status and body.
     * TODO finish
     *
     * @param request the HTTP request containing form data to be processed
     * @param headers a mapping of HTTP headers to be included in the response
     * @return a Response object representing the HTTP response, which includes the
     *         status code, headers, and body indicating the success of the operation
     */
    private static Response handleFormData(Request request, HashMap<String, String> headers) {
        String successBody = "{\"status\": \"success\"}";
        headers.putAll(buildContentHeaders(request.getPath(), successBody));
        return constructResponse(request, ResponseCode.OK, headers, successBody);
    }

    /**
     * Processes multipart data from an HTTP request and generates a corresponding response.
     * This method uses the existing form data handler to process the incoming multipart
     * form data and construct an appropriate response object.
     * TODO IMPLEMENT
     *
     * @param request the HTTP request containing multipart data to be handled
     * @param headers a mapping of HTTP headers to be included in the response
     * @return a Response object representing the HTTP response, including the status code,
     *         response headers, and body indicating the outcome of the operation
     */
    private static Response handleMultipartData(Request request, HashMap<String, String> headers) {
        return handleFormData(request, headers);
    }


    /**
     * Handles file upload requests by validating the MIME type, parsing multipart data,
     * and processing the uploaded files. Constructs appropriate HTTP responses based
     * on the success or failure of the operation.
     *
     * @param request the HTTP request containing the file upload data
     * @param contentTypeLine the Content-Type header line specifying the MIME type of the request
     * @return a Response object representing the HTTP response, which includes the status code,
     *         headers, and body indicating the outcome of the file upload operation
     */
    private static Response handleFileUpload(Request request, String contentTypeLine) {

        //constant
        final String CONTENT_TYPE_TAG = "Content-Type";

        //validate MIME type
        MIMEType contentType = MIMEType.fromHeader(contentTypeLine);
        if (!contentType.equals(MIMEType.MP_FORM_DATA)) {
            return constructErrorResponse(ResponseCode.UNSUPPORTED_MEDIA_TYPE, request);
        }

        HashMap<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE_TAG, MIMEType.APP_JSON.toString());
        ResponseCode status = ResponseCode.OK;

        try {
            HashMap<String, String> files = MultipartParser.parse(request);

            if (files.isEmpty()) {
                return constructErrorResponse(ResponseCode.BAD_REQUEST, request);
            }

            //process each file
            for (HashMap.Entry<String, String> entry : files.entrySet()) {
                //get field and file data
                String fieldName = entry.getKey().trim();
                String fileData = entry.getValue().trim();

                //validate field name
                if (fieldName.isEmpty()) {
                    return constructErrorResponse(ResponseCode.BAD_REQUEST, request);
                }
                try {
                    //upload the file
                    status = FileHandler.uploadFile(fieldName, fileData);
                    if (status.isError()) {
                        return constructErrorResponse(status, request);
                    }
                }
                catch (SecurityException e) {
                    logError("Security error when uploading file: ", e);
                    return constructErrorResponse(ResponseCode.FORBIDDEN, request);
                }
                catch (IOException e) {
                    logError("IO error when uploading file: ", e);
                    return constructErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
                }
            }
            return constructResponse(request, status, headers, "{\"status\": \"success\"}");
        }
        catch (Exception e) {
            System.err.println("Error processing file upload: " + e.getMessage());
            return constructErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    /**
     * Handles an HTTP request containing JSON data and generates an appropriate response.
     * This method constructs a basic JSON success message as the response body,
     * calculates the content length, updates the response headers, and returns the response.
     * TODO implement
     * @param request the HTTP request to be processed, containing information about the client's request
     * @param responseHeaders a map of HTTP headers to be included in the response, which will be updated
     *                        with the content length
     * @return a Response object representing the HTTP response, including the status code,
     *         headers, and a JSON-formatted body indicating a success status
     */
    private static Response handleJsonDataRequest(Request request, HashMap<String, String> responseHeaders) {

        String responseBody = "{\"status\": \"success\"}";
        responseHeaders.put("Content-Length", String.valueOf(responseBody.length()));
        return constructResponse(request, ResponseCode.OK, responseHeaders, responseBody);
    }

    /**
     * Constructs a Response with the given parameters
     * @param request The originating {@code Request}
     * @param rc the {@code ResponseCode} indicating processing status
     * @param headers A {@code HashMap<String, String>} representing the response headers
     * @param body The {@code Response} message contents
     * @return a {@code Response} object representing the appropriate response to its associated request
     */
    private static Response constructResponse(Request request, ResponseCode rc, HashMap<String, String> headers, String body) {
        validateResponseParameters(request, rc, headers, body);
        //check if an error is without a body
        if (rc.isError() && body.isEmpty()) {
            return constructErrorResponse(rc, request);
        }
        HashMap<String, String> responseHeaders = new HashMap<>(headers);
        responseHeaders.putAll(SECURITY_HEADERS);

        if (!body.isEmpty() && !headers.containsKey("Content-Length")) {
            responseHeaders.put("Content-Length", String.valueOf(body.length()));
        }

        return new Response(HTTP_VERSION, rc.toString(), responseHeaders, body, request);

    }

    /**
     * Constructs an error HTTP response based on the given error response code and request object.
     * The method ensures that the provided response code represents an error and generates a
     * response with the appropriate status, headers, and body containing the error details.
     *
     * @param rc the response code to be used for the error response, which must indicate an error state
     * @param request the HTTP request object that triggered the error, used to derive context for the response
     * @return a Response object representing the constructed error response, including the status code,
     *         headers, and an error message in the body
     * @throws IllegalArgumentException if the provided response code does not indicate an error
     */
    private static Response constructErrorResponse(ResponseCode rc, Request request) {
        if (!rc.isError()) {
            throw new IllegalArgumentException("Constructed error response with non-error response code!");
        }

        String message = rc.toString();
        return constructResponse(request, rc, new HashMap<>(), message);

    }

    /**
     * Validates the parameters for constructing an HTTP response. If any parameter is null, an exception is thrown.
     *
     * @param request the HTTP request object associated with the response
     * @param rc the response code indicating the status of the response
     * @param headers a map of HTTP headers to be included in the response
     * @param body the body content of the HTTP response
     * @throws IllegalArgumentException if any of the parameters are null
     */
    private static void validateResponseParameters(Request request, ResponseCode rc, HashMap<String, String> headers, String body) {
        if (request == null || rc == null || headers == null || body == null) {
            throw new IllegalArgumentException("Invalid response parameters!");
        }
    }

    /**
     * Logs an error message to the standard error stream.
     * This method combines the provided message and the exception's message
     * and outputs it for debugging or troubleshooting purposes.
     *
     * @param message the custom error message to log
     * @param e the exception whose details are to be logged
     */
    private static void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
    }

    /**
     * Thrown to indicate that an HTTP request is invalid or does not meet
     * the expected criteria.
     *
     * This exception is typically used in the context of validating incoming
     * HTTP requests within the {@link RBRefactor} class. It signals that the
     * request contains errors such as missing required fields, invalid formats,
     * or other conditions that render the request invalid.
     *
     * The {@code InvalidRequestException} is an unchecked exception, extending
     * {@link RuntimeException}, and can be thrown during the request processing
     * phase without the requirement to explicitly declare it in method signatures.
     */
    private static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}

/**
 * Processes form data, returning a hash map with key pairs of the fields and the data
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
                    }
                    catch (Exception e) {
                        //skip malformed entries
                        System.err.println("Malformed form data entry: " + e.getMessage());
                    }
                });

        return formData;
    }

    public static HashMap<String, String> parseMultipart(Request request) {
        return MultipartParser.parse(request);
    }
}

/**
 * Processes multipart form data, returning a hash map of the individual parts
 *  and their data
 */
class MultipartParser {
    private static final String BOUNDARY_TAG = "boundary=";
    private static final String DELIMITER = "--";
    private static final String CONTENT_DISPOSITION_TAG = "Content-Disposition";
    private static final String CONTENT_TYPE_TAG = "Content-Type";
    private static final String FORM_DATA_TAG = "form-data";

    /**
     * Parses a multipart HTTP request and extracts form fields and file data.
     * For file data, it encodes the content in Base64 and associates it with the
     * respective form field name.
     *
     * @param request the HTTP request to be parsed, including headers and body content
     * @return a HashMap containing form field names as keys and their corresponding values.
     *         For file uploads, the values are strings containing Base64-encoded file data.
     *         If the request is invalid or no data is found, an empty HashMap is returned.
     */
    public static HashMap<String, String> parse(Request request) {
        if (!RequestParser.validate(request)) {
            return new HashMap<>();
        }
        HashMap<String, String> fileData = new HashMap<>();
        try {
            String contentTypeLiteral = request.getHeaders().getOrDefault(CONTENT_TYPE_TAG, "");

            String boundary = getBoundary(contentTypeLiteral);

            if (boundary.isEmpty()) {
                return fileData;
            }
            MIMEType contentType = MIMEType.fromHeader(contentTypeLiteral);
            if (!validate(contentType)) {
                throw new IllegalArgumentException("Invalid content type for multipart request!");
            }

            /// build delimiter
            String delimiter = DELIMITER + boundary;
            String finalDelimiter = delimiter + DELIMITER;

            String bodyString = request.getBody();
            String[] parts = bodyString.split(Pattern.quote(delimiter));

            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || part.equals(DELIMITER))
                    continue;
                String[] sections = part.split("\r\n\r\n", 2);

                //skip non-partitioned entries
                if (sections.length != 2) {
                    continue;
                }

                String headerSection = sections[0].trim();
                String dataSection = sections[1].trim();

                if (headerSection.contains(finalDelimiter)) break;

                Matcher dispositionMatcher = Pattern.compile(CONTENT_DISPOSITION_TAG + ":\\s*form-data;\\s*name=\"([^\"]+)\"(?:;\\s*filename=\"([^\"]+)\")?",
                        Pattern.CASE_INSENSITIVE).matcher(headerSection);
                if (!dispositionMatcher.find()) {
                    continue;
                }
                String fieldName = dispositionMatcher.group(1);
                String filename = dispositionMatcher.group(2);

                //treat filenames as file uploads
                if (filename != null && !filename.isEmpty()) {
                    //strip trailing boundary
                    if (dataSection.endsWith(delimiter)) {
                        dataSection = dataSection.replace(delimiter, "");
                    }

                    //convert to bytes
                    byte[] data = dataSection.getBytes(StandardCharsets.UTF_8);
                    String encodedData = Base64.getEncoder().encodeToString(data);
                    fileData.put(fieldName, encodedData);
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error parsing multipart request: " + e.getMessage());
        }
        return fileData;
    }

    /**
     * Validates that the content type is within specifications
     * @param contentType The MIME type of the data stored in the request
     * @return whether the request's MIME type is valid
     */
    private static boolean validate(MIMEType contentType) {
        return contentType == MIMEType.MP_FORM_DATA;
    }

    /**
     * Every multipart form has a boundary that divides the form's parts
     * This method retrieves that boundary for use in processing
     * @param contentType The value of the 'Content-Type' header;
     *                      the boundary is located within this line, after the MIME type.
     * @return a String representing the recurring form boundary
     */
    private static String getBoundary(String contentType) {
        //validate params
        if (contentType == null || contentType.isEmpty()) {
            return "";
        }

        //split the content-type line into two around the boundary tag
        String[] parts = contentType.split(BOUNDARY_TAG);
        if (parts.length != 2) {
            return "";
        }

        if (parts[1].endsWith("\n")) {
            parts[1] = parts[1].replace("\n","");
        }
        return parts[1];
    }
}

