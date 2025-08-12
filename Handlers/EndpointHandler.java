package Handlers;

import Messages.Request;
import Messages.Response;

/**
 * The EndpointHandler interface represents a contract for handling HTTP requests.
 * Implementing classes are responsible for processing requests and generating
 * appropriate HTTP responses for specific endpoints or target resources.
 *
 * This interface can be used to define different types of handlers for various
 * HTTP resources, such as handling GET, POST, PUT, or DELETE requests or any
 * custom application logic for request processing.
 */
public interface EndpointHandler {

    /**
     * Handles the processing of an HTTP request for a specific target resource.
     * This method is responsible for generating an appropriate HTTP response
     * based on the provided request object and target.
     *
     * @param request the HTTP request object containing details such as headers,
     *                body, and source information
     * @param target the specific resource or endpoint to which the request is directed
     * @return a Response object representing the HTTP response to the processed request,
     *         including the status code, headers, and body content
     */
    Response handle(Request request, String target);
}
