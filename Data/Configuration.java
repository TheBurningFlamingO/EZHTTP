package Data;

import java.util.Set;

public class Configuration {
    private int port;
    private String rootPath;
    private String uploadPath;
    private long maxFileSize;
    private Set<String> forbiddenFileExtensions;
    private Set<Endpoint> endpoints;

    public Set<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Set<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public Set<String> getForbiddenFileExtensions() {
        return forbiddenFileExtensions;
    }

    public void setForbiddenFileExtensions(Set<String> forbiddenFileExtensions) {
        this.forbiddenFileExtensions = forbiddenFileExtensions;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Configuration() {

    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

}
