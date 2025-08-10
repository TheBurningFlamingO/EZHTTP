package Tools;

import Data.Configuration;
import Data.ResponseCode;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;

public class FileHandler {
    //constants (load from configuration)
    private static final Configuration cfg = ConfigurationManager.getInstance().getCurrentConfiguration();

    private static final String WEB_ROOT = cfg.getRootPath();
    private static final String UPLOAD_ROOT = WEB_ROOT + cfg.getUploadPath();
    private static final long MAX_FILE_SIZE = cfg.getMaxFileSize(); // 10 MB

    private static final Pattern UNSAFE_CHARACTERS = Pattern.compile("[^a-zA-Z0-9._-]");

    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(".jsp", ".php", ".exe");

    private FileHandler() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Reads the contents of a system file specified by the given file path.
     * This method reads all lines from the file and returns its content as a single string.
     *
     * @param filePath the path to the file that needs to be read
     * @return a string containing the full content of the file
     * @throws FileNotFoundException if the specified file does not exist
     * @throws IOException if an I/O error occurs during the reading process
     */
    public static String readSystemFile(String filePath) throws FileNotFoundException, IOException {
        File file = new File(filePath);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            return sb.toString();
        }
    }

    public static boolean doesResourceExist(String filePath) {
        if (!filePath.startsWith(WEB_ROOT))
            filePath = WEB_ROOT + sanitizePath(filePath);
        File file = new File(filePath);
        return file.exists() && file.isFile();
    }
    public static void postDataFile(String filePath, String content) {
        String saniPath = sanitizePath(filePath);

        //validate file path
        if (!saniPath.startsWith(WEB_ROOT)) {
            saniPath = WEB_ROOT + saniPath;
        }

        //write the file to the system
        try {
            File fileToWrite = new File(saniPath);

            switch (validateFileAccess(fileToWrite)) {
                case FORBIDDEN:
                    System.err.println("File upload forbidden for path: " + saniPath);
                    return;
                case NOT_FOUND:
                    System.err.println("File not found for path: " + saniPath);
                    if (!fileToWrite.createNewFile()) {
                        System.err.println("Failed to create file for path: " + saniPath);
                        return;
                    }
                    break;
                case INTERNAL_SERVER_ERROR:
                    System.err.println("Internal server error uploading file: " + saniPath);
                    return;
                case OK:
                    break;
            }
            writeToFile(content, fileToWrite);
        }
        catch (FileSizeException e) {
            System.err.println("File size exception uploading file: " + e.getMessage());
        }
        catch (IOException e) {
            System.err.println("Error uploading file: " + e.getMessage());
        }
        catch (SecurityException e) {
            System.err.println("Security exception uploading file: " + e.getMessage());
        }
        catch (IllegalArgumentException e) {
            System.err.println("Illegal argument exception uploading file: " + e.getMessage());
        }
        catch (Exception e) {
            System.err.println("Unexpected exception uploading file: " + e.getMessage());
        }
        finally {
            System.out.println("File uploaded successfully");
        }
    }

    /**
     * Writes the specified content to a system file at the provided file path. If the
     * file or its parent directories do not exist, they will be created automatically.
     *
     * @param filePath the full path to the file where the content will be written
     * @param content the content to be written to the specified file
     * @throws IOException if an I/O error occurs, such as failure to create the file
     *         or its parent directories, or failure during the write operation
     */
    public static void writeSystemFile(String filePath, String content) throws IOException {
         //10KB buffer
        File file = new File(filePath);

        //validate file
        if (!validateFileAccess(file).isError()) {
            throw new IOException("Could not write to file " + filePath);
        }
        //ensure parent dir exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs())
            throw new IOException("Unable to create parent directory");

        if (!file.exists() && !file.createNewFile()) {
            throw new IOException("Unable to create file");
        }

        writeToFile(content, file);
    }

    private static void writeToFile(String content, File file) throws IOException {
        final int BUFFER_SIZE = 10 * 1024;
        try (InputStream is = new ByteArrayInputStream(content.getBytes());
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
                    throw new FileSizeException("File exceeds maximum size of " + MAX_FILE_SIZE + " bytes");
                }
                out.write(buffer, 0, bytesRead);
            }
            //flush the stream
            out.flush();
        }
    }

    /**
     * Sanitizes the given file path by decoding it, removing query and fragment components,
     * and normalizing it to ensure a clean and safe structure.
     * If the input path is null, empty, or an exception occurs during processing,
     * a default root directory "/" will be returned.
     *
     * @param path the input file path to be sanitized; can be null or a potentially malformed string
     * @return the sanitized, normalized file path, or "/" if the input is invalid or an error occurs
     */
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

    /**
     * Validates if the specified file can be read based on its path and performs
     * necessary safety checks such as path sanitization and access validation.
     *
     * @param filePath the path to the file that needs to be validated for reading
     * @return a {@code ResponseCode} indicating the result of the validation,
     *         such as {@code OK} if the file is accessible or a specific error
     *         code (e.g., {@code FORBIDDEN}, {@code NOT_FOUND}, or {@code INTERNAL_SERVER_ERROR})
     */
    public static ResponseCode validateFileRead(String filePath) {

        if (!filePath.startsWith(WEB_ROOT))
            filePath = WEB_ROOT + sanitizePath(filePath);

        File file = new File(filePath);

        try {
            return validateFileAccess(file);
        }
        catch (IOException e) {
            return ResponseCode.INTERNAL_SERVER_ERROR;
        }

    }

    public static ResponseCode validateFileAccess(File file) throws IOException {
        if (file == null)
            throw new IOException("File cannot be null");

        //path traversal protection
        String canonicalPath = file.getCanonicalPath();
        String webRootPath = new File(cfg.getRootPath()).getCanonicalPath();

        if (!isFileAccessible(canonicalPath, webRootPath))
            return ResponseCode.FORBIDDEN;
        if (!file.exists() || !file.isFile())
            return ResponseCode.NOT_FOUND;

        if (hasUnsafeExtension(file))
            return ResponseCode.FORBIDDEN;

        return ResponseCode.OK;

    }

    private static boolean isFileAccessible(String filePath, String rootPath) {
        return filePath.startsWith(rootPath);
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

        final int BUFFER_SIZE = 1024 * 10;

        if (filePath == null || filePath.isEmpty() || content == null || content.isEmpty()) {
            return ResponseCode.BAD_REQUEST;
        }

        //instantiate a file
        String saniPath = sanitizePath(filePath);
        ResponseCode status = validateFileUpload(saniPath);

        if (status.isError()) {
            return status;
        }
        //write to file
        try {
            writeSystemFile(UPLOAD_ROOT + saniPath, content);
        }
        catch (FileSizeException e) {
            System.err.println("File size exception in uploadFile(): " + e.getMessage());
            return ResponseCode.INTERNAL_SERVER_ERROR;
        }
        catch (Exception e) {
            System.err.println("Exception in uploadFile(): " + e.getMessage());
            return ResponseCode.INTERNAL_SERVER_ERROR;
        }
        finally {
            System.out.println("File uploaded successfully");
        }
        return status;
    }

    public static void ensureUploadDirectoryExists() throws IOException {
        File uploadDir = new File(UPLOAD_ROOT);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Unable to create upload directory");
        }
    }

    static class FileSizeException extends IOException {
        public FileSizeException() {
        }

        public FileSizeException(String message) {
            super(message);
        }

        public FileSizeException(String message, Throwable cause) {
            super(message, cause);
        }

        public FileSizeException(Throwable cause) {
            super(cause);
        }
    }
}
