package Tools;

import Data.ResponseCode;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;

public class FileHandler {
    private static final String WEB_ROOT = "webroot";
    private static final String UPLOAD_ROOT = "upload";

    private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
            ".php", ".exe", ".sh", ".bat"
    );

    private FileHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static String sanitizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        try {
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            String cleanPath = removeQueryAndFragment(decodedPath);
            return normalizePath(cleanPath);
        }
        //return root directory if something throws
        catch (Exception e) {
            return "/";
        }
    }

    private static String removeQueryAndFragment(String path) {
        int questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex != -1) {
            path = path.substring(0, questionMarkIndex);
        }

        int hashMarkIndex = path.indexOf('#');
        if (hashMarkIndex != -1) {
            path = path.substring(0, hashMarkIndex);
        }

        return path;
    }

    private static String normalizePath(String path) {
        path = path.replace('\\', '/');
        String[] segments =  path.split("/");
        LinkedList<String> cleanSegments = new LinkedList<>();

        for (String segment : segments) {
            //remove empty segments and references to the parent directory
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }

            if ("..".equals(segment)) {
                if (!cleanSegments.isEmpty()) {
                    cleanSegments.removeLast();
                }
                continue;
            }

            segment = UNSAFE_CHARACTERS.matcher(segment).replaceAll("");
            if (!segment.isEmpty()) {
                cleanSegments.add(segment);
            }
        }
        if (cleanSegments.isEmpty()) {
            return "/";
        }

        //compose and return the result
        StringBuilder sb = new StringBuilder();
        for (String segment : cleanSegments) {
            sb.append('/');
            sb.append(segment);
        }

        return sb.toString();
    }

    public static ResponseCode validateFileAccess(File file) throws IOException {
        if (file == null)
            throw new IOException("File cannot be null");

        //path traversal protection
        String canonicalPath = file.getCanonicalPath();
        String webRootPath = new File(WEB_ROOT).getCanonicalPath();

        if (!isFileAccessible(canonicalPath, webRootPath))
            return ResponseCode.FORBIDDEN;
        if (!file.exists() || !file.isFile())
            return ResponseCode.NOT_FOUND;

        if (hasUnsafeExtension(file))
            return ResponseCode.FORBIDDEN;

        return ResponseCode.OK;

    }

    private static boolean isFileAccessible(String filePath, String rootPath) {
        return filePath.startsWith(rootPath) && new File(filePath).canRead();
    }
    private static boolean hasUnsafeExtension(File file) {
        String fileName = file.getName().toLowerCase();

        return FORBIDDEN_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    public static ResponseCode validateFileUpload(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ResponseCode.BAD_REQUEST;
        }

        File file = new File(UPLOAD_ROOT + sanitizePath(fileName));

        try {
            if (file.exists())
                return ResponseCode.CONFLICT;

            File parentDir = file.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs())
                return ResponseCode.INTERNAL_SERVER_ERROR;

            if (!parentDir.canWrite() || hasUnsafeExtension(file))
                return ResponseCode.FORBIDDEN;

            return ResponseCode.CREATED;
        }
        catch (SecurityException e) {
            return ResponseCode.FORBIDDEN;
        }
    }

    public static ResponseCode uploadFile(String filePath, String content) throws SecurityException, IOException {
        //constants
        final long MAX_FILE_SIZE = 1024 * 1024 * 10; // 10 MB
        final int BUFFER_SIZE = 1024 * 10;

        if (filePath == null || filePath.isEmpty() || content == null || content.isEmpty()) {
            return ResponseCode.BAD_REQUEST;
        }

        //instantiate a file
        String saniPath = sanitizePath(filePath);
        File file = new File(UPLOAD_ROOT + filePath);

        //validate upload
        ResponseCode status = validateFileUpload(saniPath);
        if (status != ResponseCode.CREATED) {
            return status;
        }

        //write to file

        try (InputStream is = new ByteArrayInputStream(Base64.getDecoder().decode(content));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;
            //read contents to file
            while ((bytesRead = is.read(buffer)) != -1) {
                totalBytesRead += bytesRead;

                //if file exceeds maximum size, erase and return error code
                if (totalBytesRead > MAX_FILE_SIZE) {
                    out.close();
                    Files.deleteIfExists(file.toPath());
                    return ResponseCode.REQUEST_ENTITY_TOO_LARGE;
                }
                out.write(buffer, 0, bytesRead);
            }
            //flush the stream
            out.flush();
        }

        return status;
    }

    public static void ensureUploadDirectoryExists() throws IOException {
        File uploadDir = new File(UPLOAD_ROOT);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Unable to create upload directory");
        }
    }
}
