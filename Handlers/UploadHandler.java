package Handlers;

import Data.MIMEType;
import Data.ResponseCode;
import Messages.Request;
import Messages.Response;
import Tools.ConfigurationManager;
import Tools.FileHandler;
import Tools.FileScrubber;
import Tools.MultipartParser;
import Tools.ResponseBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class UploadHandler implements EndpointHandler {
    public UploadHandler() {}
    // File scrubber for security
        private final FileScrubber fileScrubber = new FileScrubber();

    /**
     * Handles an incoming HTTP request, validates and processes multipart form data,
     * and generates an appropriate HTTP response based on the operation's outcome.
     *
     * @param request the incoming request object containing the client's request data
     * @param target a string representing the target resource; not used in this implementation
     * @return a Response object representing the server's response to the client,
     *         including status codes, headers, and any additional message body
     */
    public Response handle(Request request, String target) {//target not used in this implementation
        final String CONTENT_TYPE_TAG = "Content-Type";

        //validate MIME type
        String contentTypeLine = request.getHeaders().getOrDefault(CONTENT_TYPE_TAG, "");
        MIMEType contentType = MIMEType.fromHeader(contentTypeLine);
        if (!contentType.equals(MIMEType.MP_FORM_DATA))
            return ResponseBuilder.constructResponse(request, ResponseCode.UNSUPPORTED_MEDIA_TYPE, null, null);

        HashMap<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put(CONTENT_TYPE_TAG, MIMEType.APP_JSON.toString());
        ResponseCode status = ResponseCode.OK;

        try {
            HashMap<String, byte[]> files = MultipartParser.parse(request);
            if (files.isEmpty()) {
                return ResponseBuilder.constructResponse(request, ResponseCode.BAD_REQUEST, responseHeaders, null);
            }

            //process each file
            for (HashMap.Entry<String, byte[]> entry : files.entrySet()) {
                //get field and file data
                String fieldName = entry.getKey();
                byte[] fileData = entry.getValue();

                //validate field name
                if (fieldName.isEmpty()) {
                    return ResponseBuilder.constructResponse(request, ResponseCode.BAD_REQUEST, responseHeaders, null);
                }
                try {
                    //upload the file
                    System.out.println("Uploading file " + fieldName + " with data " + Arrays.toString(fileData));
                    status = FileHandler.uploadFile(fieldName, fileData);
                    if (status.isError()) {

                        return ResponseBuilder.constructResponse(request, status, responseHeaders, null);
                    }

                    //if the upload is successful, add a location header
                    responseHeaders.put("location", ConfigurationManager.getInstance().getCurrentConfiguration().getUploadPath() + fieldName);


                }
                catch (SecurityException e) {
                    System.err.println("Security error when uploading file: " + e.getMessage());
                    return ResponseBuilder.constructResponse(request, ResponseCode.FORBIDDEN, responseHeaders, null);
                }
                catch (IOException e) {
                    System.err.println("IO error when uploading file: " + e.getMessage());
                    return ResponseBuilder.constructResponse(request, ResponseCode.INTERNAL_SERVER_ERROR, responseHeaders, null);
                }

            }

            return ResponseBuilder.constructResponse(request, status, responseHeaders, "{\"status\": \"success\"}");

         }
        catch (Exception e) {
            System.err.println("Error processing upload request: " + e.getMessage());
            return ResponseBuilder.constructResponse(request, ResponseCode.INTERNAL_SERVER_ERROR, responseHeaders, null);
        }
    }
}
