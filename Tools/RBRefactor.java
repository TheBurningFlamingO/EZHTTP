package Tools;

import Data.ResponseCode;
import Data.MIMEType;
import Messages.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.file.*;
import java.io.*;

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
            MIMEType contentType = MIMEType.fromString(
                request.getHeaders().getOrDefault("Content-Type", "")
            );
            String path = FileHandler.sanitizePath(request.getPath());

            return switch (path) {
                case "/api/data" -> handleDataPost(request, contentType);
                case "api/upload" -> handleFileUpload(request, contentType);
                default -> constructErrorResponse(ResponseCode.NOT_FOUND, request);
            };
        }
        catch (Exception e) {
            logError("Error handling POST Request", e);
            return constructErrorResponse(ResponseCode.INTERNAL_SERVER_ERROR, request);
        }
    }

    private static boolean isInvalidPostRequest(Request request) {
        return request.getBody() == null || request.getBody().isEmpty();
    }

    private static Response handleDataPost(Request request, MIMEType contentType) {
        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", MIMEType.APP_JSON.toString());

        try {
            return switch (contentType) {
                case APP_JSON -> handleJsonDataReqeust(request, responseHeaders);
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

    private static Response handleJsonDataReqeust(Request request, HashMap<String, String> responseHeaders) {
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

class FileUploadParser {
    public static HashMap<String, String> parseFiles(Request request) {
        HashMap<String, String> files = new HashMap<>();
        HashMap<String, String> parts = MultipartParser.parse(request);
    }
}
