package Data;

import java.util.Set;

public class Configuration {
    private int port;
    private String rootPath;
    private String uploadPath;
    private long maxFileSize;
    private Set<String> forbiddenFileExtensions;
    private Set<Endpoint> endpoints;

    /**
     * Retrieves the set of all configured endpoints.
     *
     * @return a Set of Endpoint objects representing the configured endpoints.
     */
    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    /**
     * Sets the collection of endpoints for this configuration.
     *
     * @param endpoints a Set of Endpoint objects representing the application endpoints
     */
    public void setEndpoints(Set<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Retrieves the set of file extensions that are forbidden for processing or uploading in the system configuration.
     *
     * @return a Set of Strings representing the forbidden file extensions.
     */
    public Set<String> getForbiddenFileExtensions() {
        return forbiddenFileExtensions;
    }

    /**
     * Sets the collection of file extensions that are forbidden for processing or uploading in the system.
     * This method updates the configuration to restrict specific file types from being handled.
     *
     * @param forbiddenFileExtensions a Set of Strings representing the forbidden file extensions
     */
    public void setForbiddenFileExtensions(Set<String> forbiddenFileExtensions) {
        this.forbiddenFileExtensions = forbiddenFileExtensions;
    }

    /**
     * Retrieves the maximum file size allowed in the configuration.
     *
     * @return the maximum file size as a long value
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Sets the maximum file size allowed in the configuration.
     *
     * @param maxFileSize the maximum file size as a long value
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Configuration() {

    }

    /**
     * Sets the upload path for the current configuration.
     * This path is used to specify where uploaded files should be stored.
     *
     * @param uploadPath the file system path for uploads as a String
     */
    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    /**
     * Retrieves the upload path configured for the application.
     *
     * @return the upload path as a String
     */
    public String getUploadPath() {
        return uploadPath;
    }

    /**
     * Retrieves the port number configured for the application.
     *
     * @return the port number as an integer
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port number for the application configuration.
     *
     * @param port the port number as an integer
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Retrieves the root path configured for the application.
     *
     * @return the root path as a String
     */
    public String getRootPath() {
        return rootPath;
    }

    /**
     * Sets the root path for the application configuration.
     * This path specifies the base directory for the application's operations.
     *
     * @param rootPath the root path as a String
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

}
