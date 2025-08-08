package Handlers;

import Data.MIMEType;
import Messages.Request;
import Messages.Response;
import Data.ResponseCode;
import Tools.FileHandler;
import Tools.RBRefactor;
import Tools.MultipartParser;

import java.io.IOException;
import java.util.HashMap;

public class UploadHandler implements EndpointHandler {
    public UploadHandler() {}

    public Response handle(Request request) {
        final String CONTENT_TYPE_TAG = "Content-Type";

        //validate MIME type
        String contentTypeLine = request.getHeaders().getOrDefault(CONTENT_TYPE_TAG, "");
        MIMEType contentType = MIMEType.fromHeader(contentTypeLine);
        if (!contentType.equals(MIMEType.MP_FORM_DATA))
            return RBRefactor.constructResponse(request, ResponseCode.UNSUPPORTED_MEDIA_TYPE, null, null);

        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put(CONTENT_TYPE_TAG, MIMEType.APP_JSON.toString());
        ResponseCode status = ResponseCode.OK;

        try {
            HashMap<String, String> files = MultipartParser.parse(request);
            if (files.isEmpty()) {
                return RBRefactor.constructResponse(request, ResponseCode.BAD_REQUEST, responseHeaders, null);
            }

            //process each file
            for (HashMap.Entry<String, String> entry : files.entrySet()) {
                //get field and file data
                String fieldName = entry.getKey();
                String fileData = entry.getValue();

                //validate field name
                if (fieldName.isEmpty()) {
                    return RBRefactor.constructResponse(request, ResponseCode.BAD_REQUEST, responseHeaders, null);
                }
                try {
                    //upload the file
                    System.out.println("Uploading file " + fieldName + " with data " + fileData);
                    status = FileHandler.uploadFile(fieldName, fileData);
                    if (status.isError()) {

                        return RBRefactor.constructResponse(request, status, responseHeaders, null);
                    }

                }
                catch (SecurityException e) {
                    System.err.println("Security error when uploading file: " + e.getMessage());
                    return RBRefactor.constructResponse(request, ResponseCode.FORBIDDEN, responseHeaders, null);
                }
                catch (IOException e) {
                    System.err.println("IO error when uploading file: " + e.getMessage());
                    return RBRefactor.constructResponse(request, ResponseCode.INTERNAL_SERVER_ERROR, responseHeaders, null);
                }
            }

            return RBRefactor.constructResponse(request, status, responseHeaders, "{\"status\": \"success\"}");

         }
        catch (Exception e) {
            System.err.println("Error processing upload request: " + e.getMessage());
            return RBRefactor.constructResponse(request, ResponseCode.INTERNAL_SERVER_ERROR, responseHeaders, null);
        }
    }
}
