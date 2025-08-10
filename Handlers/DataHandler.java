package Handlers;

import Messages.*;
import Data.*;
import Tools.FileHandler;
import Tools.ResponseBuilder;
import java.util.HashMap;

/**
 * Handles a data POST request, saving the data to a file associated with the endpoint
 */
public class DataHandler implements EndpointHandler {
    public DataHandler() {

    }

    public Response handle(Request request, String target) {
        final String CONTENT_TYPE_TAG = "Content-Type";
        final String CONTENT_LENGTH_TAG = "Content-Length";

        //validate MIME type
        String contentTypeLine = request.getHeaders().getOrDefault(CONTENT_TYPE_TAG, "");
        MIMEType contentType = MIMEType.fromHeader(contentTypeLine);
        if (contentType != MIMEType.fromFileExtension(target))
            return ResponseBuilder.constructResponse(request, ResponseCode.UNSUPPORTED_MEDIA_TYPE, new HashMap<>(), "");

        //write to file for later access by scripts
        String bodyToWrite = request.getBody();

        try {
            System.out.println("Writing file " + target + " with data " + bodyToWrite);
            FileHandler.postDataFile(target, bodyToWrite);
        }
        catch (Exception e) {
            System.out.println("Error uploading file to server: " + e.getMessage());
            return ResponseBuilder.constructResponse(request, ResponseCode.BAD_REQUEST, new HashMap<>(), "");
        }

        //send success message
        return ResponseBuilder.constructResponse(request, ResponseCode.OK, new HashMap<>(), "{\"status\": \"success\"}");
    }

}
