package Handlers;

import Messages.Request;
import Messages.Response;

public interface EndpointHandler {
    Response handle(Request request);
}
