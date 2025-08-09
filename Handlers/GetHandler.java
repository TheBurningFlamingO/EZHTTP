package Handlers;
import Messages.*;
import Data.Configuration;
import Data.MIMEType;
import Data.ResponseCode;
import Tools.FileHandler;
import Tools.RBRefactor;
import Tools.ConfigurationManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class GetHandler implements EndpointHandler {
    private static final Configuration cfg = ConfigurationManager.getInstance().getCurrentConfiguration();

    private static final String WEB_ROOT = cfg.getRootPath();

    public GetHandler() {}

    public Response handle(Request request, String target) {
        final String DEFAULT_TARGET = "/index.html";
        String path = FileHandler.sanitizePath(target);
        path = path.equals("/") ? DEFAULT_TARGET : path;

        String filePath = WEB_ROOT + path;

        try {
            ResponseCode rc = FileHandler.validateFileRead(filePath);
            if (rc.isError()) {
                return RBRefactor.constructResponse(request, rc, null, rc.toString());
            }

            //get the file
            String fileData = FileHandler.readSystemFile(filePath).trim();
            HashMap<String, String> headers = buildContentHeaders(path, fileData);

            return RBRefactor.constructResponse(request, rc, headers, fileData);
        }
        catch (FileNotFoundException e) {
            return RBRefactor.constructResponse(request, ResponseCode.NOT_FOUND, null, null);
        }
        catch (IOException e) {
            return RBRefactor.constructResponse(request, ResponseCode.INTERNAL_SERVER_ERROR, null, null);
        }

    }

    /**
     * Builds a mapping of HTTP content headers based on the provided file path and content.
     * This method generates headers such as "Content-Type" and "Content-Length" by determining
     * the MIME type from the file extension of the given path and calculating the content's size.
     *
     * @param path the file path used to determine the MIME type
     * @param content the content whose length is used to calculate the "Content-Length" header
     * @return a HashMap containing the generated HTTP headers, including "Content-Type" and "Content-Length"
     */
    private static HashMap<String, String> buildContentHeaders(String path, String content) {
        HashMap<String, String> headers = new HashMap<>();
        MIMEType mimeType = MIMEType.fromFileExtension(path);
        headers.put("Content-Type", mimeType.toString());
        headers.put("Content-Length", String.valueOf(content.length()));
        return headers;
    }
}
