package Handlers;

import Messages.*;
import Data.*;
import Tools.FileHandler;
import Tools.MultipartParser;
import Tools.RequestParser;
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


        //write to a file for later access by scripts
        try {
            //this must be validated later
            System.out.println("Content type: " + contentType);
            switch (contentType) {
                case APP_JSON:
                    String jsonData = request.getBody();
                    FileHandler.postDataFile(target, jsonData);
                    break;
                case APP_X_WWW_FORM_URLENCODED:
                    HashMap<String, String> formData = RequestParser.getFormKeyPairs(request);
                    FileHandler.postDataFile(target, Json.stringifyPretty(Json.toJson(formData)));
                    break;
                case MP_FORM_DATA:
                    HashMap<String, String> files = MultipartParser.parse(request);
                    if (files.isEmpty()) {
                        return ResponseBuilder.constructResponse(request, ResponseCode.BAD_REQUEST, new HashMap<>(), "");
                    }
                    for (HashMap.Entry<String, String> entry : files.entrySet()) {
                        String fieldName = entry.getKey();
                        String fileData = entry.getValue();
                        FileHandler.postDataFile(fieldName, fileData);
                    }
                    break;
                default:
                    System.out.println("Unsupported content type: " + contentType);
                    return ResponseBuilder.constructResponse(request, ResponseCode.UNSUPPORTED_MEDIA_TYPE, new HashMap<>(), "");
            }
        }
        catch (Exception e) {
            System.out.println("Error uploading file to server: " + e.getMessage());
            return ResponseBuilder.constructResponse(request, ResponseCode.BAD_REQUEST, new HashMap<>(), "");
        }

        //send success message
        return ResponseBuilder.constructResponse(request, ResponseCode.OK, new HashMap<>(), "{\"status\": \"success\"}");
    }

}
