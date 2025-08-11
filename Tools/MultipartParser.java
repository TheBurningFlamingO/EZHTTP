package Tools;


import Data.MIMEType;
import Messages.Request;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes multipart form data, returning a hash map of the individual parts
 *  and their data
 */
public class MultipartParser {
    private static final String BOUNDARY_TAG = "boundary=";
    private static final String DELIMITER = "--";
    private static final String CONTENT_DISPOSITION_TAG = "Content-Disposition";
    private static final String CONTENT_TYPE_TAG = "Content-Type";
    private static final String FORM_DATA_TAG = "form-data";

    /**
     * Parses a multipart HTTP request and extracts form fields and file data.
     * For file data, it encodes the content in Base64 and associates it with the
     * respective form field name.
     *
     * @param request the HTTP request to be parsed, including headers and body content
     * @return a HashMap containing form field names as keys and their corresponding values.
     *         For file uploads, the values are strings containing Base64-encoded file data.
     *         If the request is invalid or no data is found, an empty HashMap is returned.
     */
    public static HashMap<String, byte[]> parse(Request request) {
        if (!RequestParser.validate(request)) {
            return new HashMap<>();
        }
        //return type
        HashMap<String, byte[]> fileData = new HashMap<>();
        try {
            String contentTypeLiteral = request.getHeaders().getOrDefault(CONTENT_TYPE_TAG, "");

            String boundary = getBoundary(contentTypeLiteral);

            if (boundary.isEmpty()) {
                return fileData;
            }
            MIMEType contentType = MIMEType.fromHeader(contentTypeLiteral);
            if (!validate(contentType)) {
                throw new IllegalArgumentException("Invalid content type for multipart request!");
            }

            /// build delimiter
            String delimiter = DELIMITER + boundary;
            String finalDelimiter = delimiter + DELIMITER;

            String bodyString = request.getBody();
            String[] parts = bodyString.split(Pattern.quote(delimiter));

            for (String part : parts) {
                part = part.trim();
                if (part.isEmpty() || part.equals(DELIMITER))
                    continue;
                String[] sections = part.split("\r\n\r\n", 2);

                //skip non-partitioned entries
                if (sections.length != 2) {
                    continue;
                }

                String headerSection = sections[0].trim();
                String dataSection = sections[1].trim();

                if (headerSection.contains(finalDelimiter)) break;

                Matcher dispositionMatcher = Pattern.compile(CONTENT_DISPOSITION_TAG + ":\\s*form-data;\\s*name=\"([^\"]+)\"(?:;\\s*filename=\"([^\"]+)\")?",
                        Pattern.CASE_INSENSITIVE).matcher(headerSection);
                if (!dispositionMatcher.find()) {
                    continue;
                }
                String fieldName = dispositionMatcher.group(1);
                String filename = dispositionMatcher.group(2);

                //treat filenames as file uploads
                if (filename != null && !filename.isEmpty()) {
                    //strip trailing boundary
                    if (dataSection.endsWith(delimiter)) {
                        dataSection = dataSection.replace(delimiter, "");
                    }
                    MIMEType partContentType = MIMEType.fromFileExtension(filename);
                    System.out.println("Part content type: " + partContentType.toString());
                    String contentToWrite = switch (partContentType) {
                        case TEXT_PLAIN, APP_OCTET_STREAM, APP_JSON, APP_XML, APP_X_WWW_FORM_URLENCODED -> dataSection;

                        //binary files get encoded
                        default -> Base64.getEncoder().encodeToString(dataSection.getBytes(StandardCharsets.ISO_8859_1));
                    };

                    fileData.put(filename, contentToWrite.getBytes(StandardCharsets.ISO_8859_1));
                }
            }
        }
        catch (Exception e) {
            System.err.println("Error parsing multipart request: " + e.getMessage());
        }
        return fileData;
    }

    /**
     * Validates that the content type is within specifications
     * @param contentType The MIME type of the data stored in the request
     * @return whether the request's MIME type is valid
     */
    private static boolean validate(MIMEType contentType) {
        return contentType == MIMEType.MP_FORM_DATA;
    }

    /**
     * Every multipart form has a boundary that divides the form's parts
     * This method retrieves that boundary for use in processing
     * @param contentType The value of the 'Content-Type' header;
     *                      the boundary is located within this line, after the MIME type.
     * @return a String representing the recurring form boundary
     */
    private static String getBoundary(String contentType) {
        //validate params
        if (contentType == null || contentType.isEmpty()) {
            return "";
        }

        //split the content-type line into two around the boundary tag
        String[] parts = contentType.split(BOUNDARY_TAG);
        if (parts.length != 2) {
            return "";
        }

        if (parts[1].endsWith("\n")) {
            parts[1] = parts[1].replace("\n","");
        }
        return parts[1];
    }
}


