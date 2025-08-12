package Handlers;
import Messages.Request;
import Messages.Response;
import Data.MIMEType;
import Data.ResponseCode;
import Tools.ResponseBuilder;

import java.util.HashMap;
/**
 * An echo request sends a copy of, or echoes, the request body back
 * to the sender
 */
public class EchoHandler implements EndpointHandler {

    public EchoHandler() {}

    /**
     * Handles an HTTP request by creating an HTTP response that echoes the content
     * of the request body back to the sender. The response includes the appropriate
     * Content-Type header based on the MIME type of the request body. If the MIME
     * type is binary, a binary response is constructed; otherwise, a textual response
     * is returned.
     *
     * @param request the incoming HTTP request that contains headers and body to process
     * @param target the requested target resource (not used in this implementation)
     * @return a Response object that encapsulates the echoed body, response headers, and status code
     */
    @Override
    public Response handle(Request request, String target) { //target not used in this implementation
        //get and save the MIME type of the request
        String contentTypeLine = request.getHeaders().getOrDefault("Content-Type", "");
        MIMEType contentType;
        try {
            contentType = MIMEType.fromHeader(contentTypeLine);
        }
        catch (IllegalArgumentException e) {
            contentType = MIMEType.APP_JSON;
        }
        HashMap<String, String> responseHeaders = new HashMap<>();
        byte[] respBody = request.getBody();

        responseHeaders.put("Content-Type", contentType.toString());
        responseHeaders.put("Content-Length", String.valueOf(respBody.length));
        if (contentType.isBinaryMimeType()) {
            return ResponseBuilder.constructBinaryResponse(request, ResponseCode.OK, responseHeaders, respBody);
        }
        return ResponseBuilder.constructResponse(request, ResponseCode.OK, responseHeaders, new String(respBody));
    }
}
