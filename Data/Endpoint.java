package Data;

import Handlers.*;

/**
 * Represents an endpoint in a web application or API.
 * An endpoint is defined by its HTTP method (e.g., GET, POST),
 * the path it handles, and the class responsible for handling it.
 * The handler is dynamically loaded and instantiated at runtime.
 */
public class Endpoint {
    private String path;
    private String method;
    private EndpointHandler handler;

    private String handlerClass;

    public Endpoint() {}

    /**
     * Constructs an {@code Endpoint} object with the specified HTTP method, path,
     * and handler class name.
     *
     * @param method the HTTP method that the endpoint listens to (e.g., GET, POST)
     * @param path the URL path associated with the endpoint
     * @param handlerClass the full qualified name of the class responsible for handling the request
     */
    public Endpoint(String method, String path, String handlerClass) {
        this.method = method;
        this.path = path;
        this.handlerClass = handlerClass;
    }

    /**
     * Updates the handler class name associated with the endpoint.
     *
     * @param handlerClass the full qualified name of the class responsible for handling the request
     */
    public void setHandlerClass(String handlerClass) {
        this.handlerClass = handlerClass;
    }

    /**
     * Retrieves the fully qualified name of the handler class associated with the endpoint.
     *
     * @return the name of the handler class as a {@code String}
     */
    public String getHandlerClass() {
        return handlerClass;
    }

    /**
     * Retrieves the handler object associated with this endpoint.
     * The handler is responsible for processing HTTP requests directed to this endpoint.
     *
     * @return the {@code EndpointHandler} instance representing the handler for this endpoint,
     *         or {@code null} if the handler has not been instantiated or could not be initialized.
     */
    public EndpointHandler getHandler() {
        return handler;
    }

    /**
     * Updates the handler for the endpoint by dynamically loading and instantiating
     * the specified handler class. The handler is responsible for processing
     * incoming HTTP requests directed to this endpoint. If the instantiation fails,
     * the handler will be set to null, and an error message will be logged.
     *
     * @param handlerClass the fully qualified name of the class implementing
     *                     {@code EndpointHandler} that will handle requests
     *                     for this endpoint
     */
    public void setHandler(String handlerClass) {
        this.handlerClass = handlerClass;
        try {
            this.handler = (EndpointHandler) Class.forName(handlerClass).getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            System.err.println("Error instantiating handler class: " + e.getMessage());
            e.printStackTrace();
            this.handler = null;
        }
    }

    /**
     * Retrieves the HTTP method associated with the endpoint.
     *
     * @return the HTTP method as a {@code String}, typically representing
     *         standard methods such as GET, POST, PUT, DELETE, etc.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Updates the HTTP method associated with this endpoint.
     *
     * @param method the HTTP method for the endpoint (e.g., GET, POST, PUT, DELETE)
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Retrieves the URL path associated with the endpoint.
     *
     * @return the URL path as a {@code String}
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the URL path associated with the endpoint.
     *
     * @param path the URL path to be associated with this endpoint
     */
    public void setPath(String path) {
        this.path = path;
    }
}
