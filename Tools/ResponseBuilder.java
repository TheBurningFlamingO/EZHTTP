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
     * Builds an HTTP response for the given request by validating the request
     * and routing it through the proper handler.
     *
     * @param request the HTTP request object to be processed into a response
     * @return a {@code Response} object representing the HTTP response generated
     *         based on the given request
     * @throws InvalidRequestException if the request fails validation
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
     * Constructs an HTTP response based on the provided parameters. This method validates
     * the input, applies required security headers, and computes the response body and its
     * associated headers. If a body is not supplied for an error response code, it will
     * generate a default error response.
     *
     * @param request the HTTP request object that triggered this response, used to retain context
     * @param rc the {@code ResponseCode} representing the HTTP status code of the response
     * @param headers a {@code HashMap<String, String>} containing the HTTP headers for the response
     * @param body the body content of the HTTP response as a string. If null, it will be treated as an empty string
     * @return a {@code Response} object containing the HTTP response, including the status, headers, and body
     * @throws IllegalArgumentException if any input parameter is invalid or null
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
     * Constructs a binary HTTP response based on the provided parameters.
     * This method validates the inputs, applies necessary headers, and builds
     * a {@link Response} object containing the binary body.
     *
     * @param request the HTTP request that triggered this response, used to retain context
     * @param rc the {@code ResponseCode} indicating the status of the response
     * @param headers a {@code HashMap<String, String>} containing the HTTP headers for the response
     * @param body a byte array representing the binary content of the response's body
     * @return a {@code Response} object containing the binary response, including HTTP version,
     *         headers, and the binary body
     * @throws IllegalArgumentException if any input parameter is invalid or null
     */
    public static Response constructBinaryResponse(Request request, ResponseCode rc, HashMap<String, String> headers, byte[] body) {
        validateResponseParameters(request, rc, headers, null);
        HashMap<String, String> responseHeaders = new HashMap<>(headers);
        responseHeaders.putAll(SECURITY_HEADERS);

        responseHeaders.put("Content-Length", String.valueOf(body.length));

        return new Response(request, HTTP_VERSION, rc, responseHeaders, body);
    }


    /**
     * Constructs an HTTP error response based on the specified response code
     * and request. This method ensures that the provided response code
     * represents an error condition and generates a corresponding response object.
     *
     * @param rc the {@code ResponseCode} representing the HTTP error status code
     *           for the response; must indicate an error.
     * @param request the HTTP request object associated with the error response.
     * @return a {@code Response} object representing the HTTP error response,
     *         including the error status and a default error message.
     * @throws IllegalArgumentException if the provided {@code ResponseCode}
     *         does not indicate an error condition.
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


            return handler.handle(request, ep.getPath());

        }
    }
}

