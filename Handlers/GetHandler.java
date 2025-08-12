package Handlers;
import Messages.*;
import Data.Configuration;
import Data.MIMEType;
import Data.ResponseCode;
import Tools.FileHandler;
import Tools.ResponseBuilder;
import Tools.ConfigurationManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * Handles HTTP GET requests by serving static files from the web root directory.
 * This class implements the {@link EndpointHandler} interface and processes GET
 * requests to deliver requested resources. If a directory is requested ("/"),
 * it defaults to serving the "index.html" file.
 *
 * The handler reads the file system to locate and return the requested file.
 * If the file is found and valid, the handler generates an appropriate HTTP response
 * containing the file content and associated headers such as MIME type and content length.
 * Files with binary MIME types are returned as binary data; otherwise, they are returned as text.
 *
 * Error scenarios such as file not found or I/O exceptions are also handled, and
 * corresponding HTTP error responses are generated.
 *
 * This class uses configuration data from the {@link ConfigurationManager} to
 * determine the root directory of the web server.
 */
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
                return ResponseBuilder.constructResponse(request, rc, new HashMap<>(), rc.toString());
            }

            MIMEType mime = MIMEType.fromFileExtension(filePath);
            if (mime.isBinaryMimeType()) {
                System.out.println("Reading file " + filePath + " as binary");
                byte[] fileData = FileHandler.readSystemFileAsBytes(filePath);
                return ResponseBuilder.constructBinaryResponse(request, ResponseCode.OK, buildContentHeaders(filePath, fileData), fileData);
            }
            else {
                System.out.println("Reading file " + filePath + " as text");
                String content = FileHandler.readSystemFileAsString(filePath);

                System.out.println("File content: " + content);
                return ResponseBuilder.constructResponse(request, ResponseCode.OK, buildContentHeaders(filePath, content), content);
            }
        }
        catch (FileNotFoundException e) {
            return ResponseBuilder.constructResponse(request, ResponseCode.NOT_FOUND, null, null);
        }
        catch (IOException e) {
            return ResponseBuilder.constructResponse(request, ResponseCode.INTERNAL_SERVER_ERROR, null, null);
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

    private static HashMap<String, String> buildContentHeaders(String path, byte[] content) {
        HashMap<String, String> headers = new HashMap<>();
        MIMEType mimeType = MIMEType.fromFileExtension(path);
        headers.put("Content-Type", mimeType.toString());
        headers.put("Content-Length", String.valueOf(content.length));
        return headers;
    }
}
