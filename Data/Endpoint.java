package Data;

import Handlers.*;

public class Endpoint {
    private String path;
    private String method;
    private EndpointHandler handler;

    private String handlerClass;

    public Endpoint() {}

    public Endpoint(String method, String path, String target, String handlerClass) {
        this.method = method;
        this.path = path;
        this.handlerClass = handlerClass;
    }

    public void setHandlerClass(String handlerClass) {
        this.handlerClass = handlerClass;
    }

    public String getHandlerClass() {
        return handlerClass;
    }

    public EndpointHandler getHandler() {
        return handler;
    }

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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
