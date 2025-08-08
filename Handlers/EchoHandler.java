package Handlers;
import Messages.Request;
import Messages.Response;
import Data.MIMEType;
import Data.ResponseCode;
import Tools.RBRefactor;

import java.util.HashMap;
/**
 * An echo request sends a copy of, or echoes, the request body back
 * to the sender
 */
public class EchoHandler implements EndpointHandler {
    @Override
    public Response handle(Request request) {
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
        String respBody = request.getBody();
        responseHeaders.put("Content-Type", contentType.toString());
        responseHeaders.put("Content-Length", String.valueOf(respBody.length()));
        return RBRefactor.constructResponse(request, ResponseCode.OK, responseHeaders, respBody);
    }
}
