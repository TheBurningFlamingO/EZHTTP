package Tools;

import java.util.UUID;

public class FileScrubber {
    // Configuration:
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    // Define the magic number signatures for our allowed file types
    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF_SIGNATURE = new byte[]{(byte) 0x25, 0x50, 0x44, 0x46, 0x2D}; // "%PDF-"

    public static class ScrubbingException extends RuntimeException {
        public ScrubbingException(String message) { super(message); }
    }

    public static class ScrubbedFile {
        public final byte[] content;
        public final String safeFilename;
        public ScrubbedFile(byte[] content, String safeFilename) {
            this.content = content;
            this.safeFilename = safeFilename;
        }
    }

    public ScrubbedFile scrubAndValidate(byte[] content) {
        if (content == null || content.length == 0) {
            throw new ScrubbingException("File content is empty.");
        }
        if (content.length > MAX_FILE_SIZE_BYTES) {
            throw new ScrubbingException("File size exceeds limit.");
        }

        // 
        String detectedMimeType = getMimeType(content);

        if (detectedMimeType == null) {
            throw new ScrubbingException("Invalid or disallowed file type detected.");
        }

        String safeFilename = generateSafeFilename(detectedMimeType);
        return new ScrubbedFile(content, safeFilename);
    }

    /**
     * Manually checks the first few bytes of a file to determine its type.
     * @param content The raw byte array of the file.
     * @return The MIME type string or null if not recognized.
     */
    private String getMimeType(byte[] content) {
        if (startsWith(content, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (startsWith(content, JPEG_SIGNATURE)) {
            return "image/jpeg";
        }
        if (startsWith(content, PDF_SIGNATURE)) {
            return "application/pdf";
        }
        // If no signature matches, it's an unknown/disallowed type
        return null;
    }

    /**
     * Helper method to check if a byte array starts with a specific sequence of bytes.
     */
    private boolean startsWith(byte[] source, byte[] pattern) {
        if (source == null || pattern == null || source.length < pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (source[i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private String generateSafeFilename(String mimeType) {
        String extension = ".dat";
        if ("image/jpeg".equals(mimeType)) extension = ".jpg";
        if ("image/png".equals(mimeType)) extension = ".png";
        if ("application/pdf".equals(mimeType)) extension = ".pdf";
        return UUID.randomUUID().toString() + extension;
    }
}