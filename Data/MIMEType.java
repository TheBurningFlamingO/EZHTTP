package Data;

import java.util.HashMap;

public enum MIMEType {
    TEXT_HTML("text/html"),
    TEXT_PLAIN("text/plain"),
    TEXT_CSS("text/css"),
    APP_OCTET_STREAM("application/octet-stream"),
    APP_JS("application/javascript"),
    APP_JSON("application/json"),
    APP_XML("application/xml"),
    APP_X_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    IMAGE_GIF("image/gif"),
    IMAGE_SVG("image/svg+xml"),
    IMAGE_BMP("image/bmp"),
    IMAGE_ICO("image/x-icon"),
    IMAGE_WEBP("image/webp"),
    AUDIO_MP3("audio/mpeg"),
    AUDIO_OGG("audio/ogg"),
    MP_FORM_DATA("multipart/form-data"),
    MP_MIXED("multipart/mixed"),
    APP_PDF("application/pdf"),
    APP_ZIP("application/zip");

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
        if (filePath.endsWith(".png"))
            return IMAGE_PNG;
        if (filePath.endsWith(".jpg"))
            return IMAGE_JPEG;
        if (filePath.endsWith(".gif"))
            return IMAGE_GIF;
        if (filePath.endsWith(".svg"))
            return IMAGE_SVG;
        if (filePath.endsWith(".bmp"))
            return IMAGE_BMP;
        if (filePath.endsWith(".ico"))
            return IMAGE_ICO;
        if (filePath.endsWith(".webp"))
            return IMAGE_WEBP;
        if (filePath.endsWith(".mp3"))
            return AUDIO_MP3;
        if (filePath.endsWith(".ogg"))
            return AUDIO_OGG;
        if (filePath.endsWith(".pdf"))
            return APP_PDF;
        if (filePath.endsWith(".zip"))
            return APP_ZIP;

        return APP_OCTET_STREAM;
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


    /**
     * Determines if a MIME type represents binary content
     */
    public boolean isBinaryMimeType() {
        return switch (this) {
            case IMAGE_JPEG, IMAGE_PNG, IMAGE_GIF, IMAGE_ICO, IMAGE_SVG,
                 APP_PDF, APP_OCTET_STREAM, APP_ZIP -> true;
            default -> false;
        };
    }


    public String toFileExension() {
        return switch (this) {
            case TEXT_HTML -> ".html";
            case TEXT_CSS -> ".css";
            case TEXT_PLAIN -> ".txt";
            case APP_JS -> ".js";
            case APP_JSON -> ".json";
            case APP_XML -> ".xml";
            case IMAGE_PNG -> ".png";
            case IMAGE_JPEG -> ".jpg";
            case IMAGE_GIF -> ".gif";
            case IMAGE_SVG -> ".svg";
            case IMAGE_BMP -> ".bmp";
            case IMAGE_ICO -> ".ico";
            case IMAGE_WEBP -> ".webp";
            case AUDIO_MP3 -> ".mp3";
            case AUDIO_OGG -> ".ogg";
            default -> "";
        };
    }
}