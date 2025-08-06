package Tools;

import Data.ResponseCode;
import Data.MIMEType;
import Messages.*;

import javax.print.DocFlavor;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RBRefactor {
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

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            "php", "asp", "aspx", "jsp", "php3", "php4", "phtml", "exe", "bat", "sh", "dll", "py", "pl", "rb"
    );

    private RBRefactor() {
        throw new IllegalStateException("Utility class");
    }

    public static Response buildResponse(Request request) {
        validateRequest(request);
        return switch (request.getMethod()) {
            case "GET" -> handleGetRequest(request);
            case "POST" -> handlePostRequest(request);
            default -> constructErrorResponse(ResponseCode.METHOD_NOT_ALLOWED, request);
        };
    }

    private static void validateRequest(Request request) {
        if (request == null) {
            throw new InvalidRequestException("Request cannot be null!");
        }
    }

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

    private static HashMap<String, String> buildContentHeaders(String path, String content) {
        HashMap<String, String> headers = new HashMap<>();
        MIMEType mimeType = MIMEType.fromFileExtension(path);
        headers.put("Content-Type", mimeType.toString());
        headers.put("Content-Length", String.valueOf(content.length()));
        return headers;
    }

    private static Response handlePostRequest(Request request) {
        //ensure request is valid
        validateRequest(request);
        if (isInvalidPostRequest(request))
            return constructErrorResponse(ResponseCode.BAD_REQUEST, request);

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

    private static Response handleEcho(Request request, MIMEType contentType) {
        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", contentType.toString());
        responseHeaders.put("Content-Length", String.valueOf(request.getBody().length()));

        return constructResponse(request, ResponseCode.OK, responseHeaders, request.getBody());
    }

    private static boolean isInvalidPostRequest(Request request) {
        return request.getBody() == null || request.getBody().isEmpty();
    }

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

    private static Response handleFormData(Request request, HashMap<String, String> headers) {
        String successBody = "{\"status\": \"success\"}";
        headers.putAll(buildContentHeaders(request.getPath(), successBody));
        return constructResponse(request, ResponseCode.OK, headers, successBody);
    }

    private static Response handleMultipartData(Request request, HashMap<String, String> headers) {
        return handleFormData(request, headers);
    }


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

    private static Response handleJsonDataRequest(Request request, HashMap<String, String> responseHeaders) {

        String responseBody = "{\"status\": \"success\"}";
        responseHeaders.put("Content-Length", String.valueOf(responseBody.length()));
        return constructResponse(request, ResponseCode.OK, responseHeaders, responseBody);
    }

    private static Response constructResponse(Request request, ResponseCode rc, HashMap<String, String> headers, String body) {
        validateResponseParameters(request, rc, headers, body);
        HashMap<String, String> responseHeaders = new HashMap<>(headers);
        responseHeaders.putAll(SECURITY_HEADERS);

        if (!body.isEmpty() && !headers.containsKey("Content-Length")) {
            responseHeaders.put("Content-Length", String.valueOf(body.length()));
        }

        return new Response(HTTP_VERSION, rc.toString(), responseHeaders, body, request);

    }

    private static Response constructErrorResponse(ResponseCode rc, Request request) {
        if (!rc.isError()) {
            throw new IllegalArgumentException("Constructed error response with non-error response code!");
        }

        String message = rc.toString();
        return constructResponse(request, rc, new HashMap<>(), message);

    }

    private static void validateResponseParameters(Request request, ResponseCode rc, HashMap<String, String> headers, String body) {
        if (request == null || rc == null || headers == null || body == null) {
            throw new IllegalArgumentException("Invalid response parameters!");
        }
    }

    private static void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
    }
    private static class InvalidRequestException extends RuntimeException {
        public InvalidRequestException(String message) {
            super(message);
        }
    }
}

class FormDataParser {
    public static HashMap<String, String> parseUrlEncoded(String body) {
        HashMap<String, String> formData = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return formData;
        }

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

class MultipartParser {
    private static final String BOUNDARY_TAG = "boundary=";
    private static final String DELIMITER = "--";
    private static final String CONTENT_DISPOSITION_TAG = "Content-Disposition";
    private static final String CONTENT_TYPE_TAG = "Content-Type";
    private static final String FORM_DATA_TAG = "form-data";

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
    private static boolean validate(MIMEType contentType) {
        return contentType == MIMEType.MP_FORM_DATA;
    }

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

