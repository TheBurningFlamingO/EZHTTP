package Handlers;

import Messages.*;
import Data.*;
import Tools.FileHandler;
import Tools.MultipartParser;
import Tools.RequestParser;
import Tools.ResponseBuilder;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles a data POST request, saving the data to a file associated with the endpoint
 */
public class DataHandler implements EndpointHandler {
    public DataHandler() {

    }

    public Response handle(Request request, String target) {
        final String CONTENT_TYPE_TAG = "Content-Type";


        //validate MIME type
        String contentTypeLine = request.getHeaders().getOrDefault(CONTENT_TYPE_TAG, "");
        MIMEType contentType = MIMEType.fromHeader(contentTypeLine);

        //write to a file for later access by scripts
        try {
            //this must be validated later
            System.out.println("Content type: " + contentType);
            String resourceName = resolveResourceName(target);
            switch (contentType) {
                case TEXT_PLAIN:
                case APP_JSON:
                    byte[] jsonData = request.getBody();
                    FileHandler.postDataFile(resourceName + ".json", jsonData);
                    break;
                case APP_X_WWW_FORM_URLENCODED:
                    HashMap<String, String> formData = RequestParser.getFormKeyPairs(request);
                    String contentToWrite = Json.stringifyPretty(Json.toJson(formData));

                    FileHandler.postDataFile(resourceName + ".json", Base64.getEncoder().encode(contentToWrite.getBytes()));
                    break;
                case APP_OCTET_STREAM:
                case MP_FORM_DATA:
                    HashMap<String, byte[]> files = MultipartParser.parse(request);

                    if (files.isEmpty()) {
                        return ResponseBuilder.constructResponse(request, ResponseCode.BAD_REQUEST, new HashMap<>(), "");
                    }
                    for (HashMap.Entry<String, byte[]> entry : files.entrySet()) {
                        String fileName = entry.getKey();
                        byte[] fileData = entry.getValue();

                        FileHandler.postDataFile(resourceName + "/" + fileName, fileData);
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

    private String resolveResourceName(String path) {
        int suffix = 0;
        String resourceName = path + "/resource";
        //find next available resource
        while (FileHandler.doesResourceExist(resourceName + suffix)) {
            suffix++;
        }
        return resourceName + suffix;
    }

}
