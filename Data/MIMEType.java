package Data;

import java.util.HashMap;

public enum MIMEType {
    TEXT_HTML("text/html"),
    TEXT_PLAIN("text/plain"),
    TEXT_CSS("text/css"),
    APP_JS("application/javascript"),
    APP_JSON("application/json"),
    APP_XML("application/xml"),
    APP_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_GIF("image/gif"),
    IMAGE_SVG("image/svg+xml"),
    IMAGE_BMP("image/bmp"),
    IMAGE_ICO("image/vnd.microsoft.icon"),
    IMAGE_WEBP("image/webp"),
    AUDIO_MP3("audio/mpeg"),
    AUDIO_OGG("audio/ogg"),
    MP_FORM_DATA("multipart/form-data"),
    MP_MIXED("multipart/mixed");

    private final String type;

    MIMEType(String type) {
        this.type = type;
    }

    public String toString() {
        return type;
    }

    /**
     * Converts the given string representation of a MIME type into a corresponding {@code MIMEType} enum constant.
     *
     * @param filePath the string representation of the MIME type to be converted
     * @return the matching {@code MIMEType} enum constant
     * @throws IllegalArgumentException if the given string does not match any known MIME type
     */
    public static MIMEType fromFileExtension(String filePath) throws IllegalArgumentException {

        if (filePath.endsWith(".html"))
            return TEXT_HTML;
        if (filePath.endsWith(".css"))
            return TEXT_CSS;
        if (filePath.endsWith(".txt"))
            return TEXT_PLAIN;
        if (filePath.endsWith(".js"))
            return APP_JS;
        if (filePath.endsWith(".json"))
            return APP_JSON;
        if (filePath.endsWith(".xml"))
            return APP_XML;

        throw new IllegalArgumentException("Unknown MIME type: " + filePath + "!");
    }

    public static MIMEType fromString(String type) throws IllegalArgumentException {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("MIME type cannot be null or empty!");
        }
        String normalizedType = type.trim().toLowerCase();

        for (MIMEType mt : values()) {
            if (mt.toString().toLowerCase().equals(normalizedType))
                return mt;
        }
        throw new IllegalArgumentException("Unknown MIME type: " + type + "!");
    }

    public static MIMEType fromHeader(String contentTypeLine) throws IllegalArgumentException {
        //trim divider and boundary from the header line
        if (contentTypeLine.contains(";")) {
            contentTypeLine = contentTypeLine.substring(0, contentTypeLine.indexOf(";"));
        }

        return fromString(contentTypeLine.trim());
    }
}