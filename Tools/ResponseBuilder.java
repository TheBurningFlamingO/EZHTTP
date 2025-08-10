package Tools;

import Data.Configuration;
import Data.Endpoint;
import Data.ResponseCode;
import Data.MIMEType;
import Messages.*;
import Handlers.*;

import java.util.*;

/**
 * Response Builder system
 * Processes an HTTP request and constructs an appropriate response
 */
public class ResponseBuilder {
    //constants - todo make configurable (besides the HTTP version)
    private static final Configuration cfg = ConfigurationManager.getInstance().getCurrentConfiguration();

    //trying out the new configuration here - seems to work (Reilly)
    private static final String HTTP_VERSION = "HTTP/1.1";

    private static final Map<String, String> SECURITY_HEADERS = Map.of(
            "X-Content-Type-Options", "nosniff",
            "X-XSS-Protection", "1; mode=block",
            "X-Frame-Options", "DENY",
            "Content-Security-Policy", "default-src 'self'",
            "Strict-Transport-Security", "max-age=31536000; includeSubDomains",
            "Referrer-Policy", "strict-origin-when-cross-origin"
    );

    //cannot instantiate utility class
    private ResponseBuilder() {
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

        return Router.getInstance().route(request);
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
     * Constructs a Response with the given parameters
     * @param request The originating {@code Request}
     * @param rc the {@code ResponseCode} indicating processing status
     * @param headers A {@code HashMap<String, String>} representing the response headers
     * @param body The {@code Response} message contents
     * @return a {@code Response} object representing the appropriate response to its associated request
     */
    public static Response constructResponse(Request request, ResponseCode rc, HashMap<String, String> headers, String body) {
        validateResponseParameters(request, rc, headers, body);
        //check if an error is without a body
        if (body == null) body = "";
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
        if (request == null || rc == null)
            throw new IllegalArgumentException("Neither request nor response code may be null!");
        if (headers == null)
            headers = new HashMap<>();
        if (body == null)
            body = "";
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
     * HTTP requests within the {@link ResponseBuilder} class. It signals that the
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

    static class Router {

        private final HashMap<String, Endpoint> getHandlers = new HashMap<>();
        private final HashMap<String, Endpoint> postHandlers = new HashMap<>();


        private static final Router instance = new Router();

        public static Router getInstance() {
            return instance;
        }

        //todo load the registry through the configuration
        public Router() {
            Set<Endpoint> endpoints = cfg.getEndpoints();

            for (Endpoint endpoint : endpoints) {
                this.register(endpoint);
            }
        }

        public void register(Endpoint endpoint) {
            switch (endpoint.getMethod()) {
                case "GET" -> getHandlers.put(endpoint.getPath(), endpoint);
                case "POST" -> postHandlers.put(endpoint.getPath(), endpoint);
                default -> System.err.println("Invalid endpoint method: " + endpoint.getMethod());
            }
        }



        public Response route(Request request) {
            String path =  FileHandler.sanitizePath(request.getPath());

            String method = request.getMethod();
            Endpoint ep = switch (method) {
                case "GET" -> getHandlers.get(path);
                case "POST" -> postHandlers.get(path);
                default -> null;
            };
            if (ep == null) {
                System.err.println("No endpoint found for " + method + " request to " + path);
                return constructErrorResponse(ResponseCode.NOT_FOUND, request);
            }

            //handle the request
            EndpointHandler handler = ep.getHandler();
            if (handler == null) {
                System.err.println("No handler found for " + method + " request to " + path);
                return constructErrorResponse(ResponseCode.NOT_FOUND, request);
            }


            return handler.handle(request, ep.getTarget());

        }
    }
}

