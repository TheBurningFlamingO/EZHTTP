package Tools;

import Data.*;
import Handlers.EchoHandler;
import Handlers.EndpointHandler;
import Handlers.GetHandler;
import Handlers.UploadHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Base64;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfigurationManager {

    private static ConfigurationManager instance;
    private static Configuration config;

    private ConfigurationManager() {
//        loadConfigurationFromFile("config/config.json");
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }

        return instance;
    }



    public void loadConfigurationFromFile(String filePath) throws IOException {
        try {
            String configSrc = FileHandler.readSystemFileAsString(filePath);
            JsonNode cfgNode = Json.parse(configSrc);
            config = Json.fromJson(cfgNode, Configuration.class);

            for (Endpoint ep : config.getEndpoints()) {
                if (ep.getHandlerClass() != null) {
                    ep.setHandler(ep.getHandlerClass());
                }
            }
        }
        //generate a default configuration file if one does not already exist
        catch (FileNotFoundException e) {
            System.out.println("No configuration file found. Generating default configuration...");
            generateDefaultConfiguration();
        }
    }

    /**
     * Generates the default configuration for the application and writes it to a configuration file.
     *
     * The method creates a new {@link Configuration} object with default values, including:
     * - Default port value as 8080.
     * - Default root path as "webroot".
     * - Default upload endpoint path as "/api/upload".
     * - Maximum allowable file size as 10 MB.
     * - Disallowed file extensions for uploads, including potentially unsafe extensions.
     *
     * Once the configuration object is created, it is serialized into JSON format and
     * written to a configuration file located at "config/config.json". If any error
     * occurs during the serialization or write process, it throws a {@link ConfigurationException}
     * wrapping the original exception.
     *
     * This method ensures that the configuration is set in memory even if file operations fail.
     * The resulting configuration is assigned to the class-level {@code config} instance.
     *
     * @throws ConfigurationException If the configuration cannot be serialized or written to the file.
     */
    private void generateDefaultConfiguration() {
        Configuration conf = new Configuration();
        conf.setPort(8080);
        conf.setRootPath("webroot");
        conf.setUploadPath("/api/upload");
        conf.setMaxFileSize(1024 * 1024 * 10);  //10 MB
        conf.setForbiddenFileExtensions(Set.of( "php", "asp", "aspx", "jsp", "php3", "php4", "phtml", "exe", "bat", "sh", "dll", "py", "pl", "rb"));
        conf.setEndpoints(Set.of());

        String cfgFilePath = "config/config.json";
        try {
            //write configuration to file
            JsonNode cfgNode = Json.toJson(conf);
            String srcToWrite = Json.stringifyPretty(cfgNode);
            FileHandler.writeSystemFile(cfgFilePath, Base64.getEncoder().encode(srcToWrite.getBytes()));
        }
        catch (JsonProcessingException e) {
            throw new ConfigurationException("Failed to generate default configuration!", e);
        }
        catch (IOException e) {
            throw new ConfigurationException("Failed to write default configuration to file!", e);
        }
        finally {
            config = conf;
        }
    }

    /**
     * Provides the current configuration instance for the application.
     *
     * If the configuration has not been initialized, it attempts to load the configuration
     * from a file. If the file does not exist or cannot be loaded, this method generates
     * a default configuration and uses it as the current configuration.
     *
     * @return the current {@link Configuration} object used by the application
     */
    public Configuration getCurrentConfiguration() {
        if (config == null) {
            try {
                loadConfigurationFromFile("config/config.json");
            }
            catch (IOException e) {
                System.err.println("Failed to load configuration file: " + e.getMessage());
                generateDefaultConfiguration();
            }
        }
        return config;
    }

    static class ConfigurationException extends RuntimeException {
        public ConfigurationException(String message) {
            super(message);
        }
        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
        public ConfigurationException(Throwable cause) {
            super(cause);
        }
        public ConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
        public ConfigurationException() {}

    }
}
