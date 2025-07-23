package Tools;
import Messages.*;
import java.util.HashMap;
import java.nio.file.Files;
import java.io.*;


public class ResponseBuilder {
    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final String WEB_ROOT = "webroot"; //directory for static files

    private static final HashMap<Integer, String> RESPONSE_CODES = new HashMap<>() {{
        put(200, "OK");
        put(201, "Created");
        put(204, "No Content");
        put(400, "Bad Request");
        put(401, "Unauthorized");
        put(403, "Forbidden");
        put(404, "Not found");
        put(405, "Method Not Allowed");
        put(500, "Internal Server Error");
        put(503, "Service Unavailable");
    }};

    public ResponseBuilder() {
        throw new IllegalStateException("Attempt to instantiate static class");

    }


    /**
     * Placeholder logic for now.
     * @param request, the target request
     * @return An appropriate response
     */
    public static Response buildResponse(Request request) {
        return switch (request.getMethod()) {
            case "GET" -> handleGetRequest(request);
            case "POST" -> handlePostRequest(request);
            default -> buildErrorResponse(405, request);
        };
    }

    private static Response handleGetRequest(Request request) {
        String path = request.getPath();

        //Sanitize path to prevent directory traversal attack
        path = path.replaceAll("\\.\\.", "");

        //Serve index.html for the root path.
        if (path.equals("/")) {
            path = "/index.html";
        }

        File file = new File(WEB_ROOT + path);
        int responseCode = 0;
        if (!file.exists()) {
            responseCode = 404;
            return buildErrorResponse(responseCode, request);
        }

        //read file contents
        try {
            byte[] content = Files.readAllBytes(file.toPath());
            //set up headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", getMimeType(path));
            headers.put("Content-Length", String.valueOf(content.length));
            responseCode = 200;
            return new Response(String.valueOf(responseCode), HTTP_VERSION, headers, new String(content), request.getSocket());

        }
        catch (IOException e) {
            responseCode = 500;
            return buildErrorResponse(responseCode, request);
        }
    }

    private static Response handlePostRequest(Request request) {
        // POST request handling logic to be added
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        //acknowledge receipt of request for now
        String responseBody = "{\"status\": \"received\"";
        headers.put("Content-Length", String.valueOf(responseBody.length()));

        return new Response("200", HTTP_VERSION, headers, responseBody, request.getSocket());


    }
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
    private static Response buildErrorResponse(int code, Request req) {
        HashMap<String, String> headers = new HashMap<>();
        String message = RESPONSE_CODES.get(code);
        headers.put("Content-Type", "text/plain");
        headers.put("Content-Length", String.valueOf(message.length()));

        return new Response(Integer.toString(code), HTTP_VERSION, headers, message, req.getSocket());
    }
}
