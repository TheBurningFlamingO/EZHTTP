package Data;

import java.util.HashMap;

public enum MIMEType {
    TEXT_HTML ("text/html"),
    TEXT_PLAIN ("text/plain"),
    TEXT_CSS ("text/css"),
    APP_JS ("application/javascript"),
    APP_JSON ("application/json"),
    APP_XML ("application/xml"),
    APP_X_WWW_FORM_URLENCODED ("application/x-www-form-urlencoded"),
    IMAGE_PNG ("image/png"),
    IMAGE_JPEG ("image/jpeg"),
    IMAGE_GIF ("image/gif"),
    IMAGE_SVG ("image/svg+xml"),
    IMAGE_BMP ("image/bmp"),
    IMAGE_ICO ("image/vnd.microsoft.icon"),
    IMAGE_WEBP ("image/webp"),
    AUDIO_MP3 ("audio/mpeg"),
    AUDIO_OGG ("audio/ogg"),
    MP_FORM_DATA ("multipart/form-data"),
    MP_MIXED ("multipart/mixed");

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
     * @param type the string representation of the MIME type to be converted
     * @return the matching {@code MIMEType} enum constant
     * @throws IllegalArgumentException if the given string does not match any known MIME type
     */
    public static MIMEType fromString(String type) throws IllegalArgumentException {
        for (MIMEType mt : values()) {
            if (mt.toString().equals(type))
                return mt;
        }
        throw new IllegalArgumentException("Unknown MIME type: " + type + "!");
    }
}
