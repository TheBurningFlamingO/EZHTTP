package Tools;

import Data.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * The {@code ConfigurationManager} class is responsible for managing the application's
 * configuration settings. This class follows the Singleton design pattern to ensure that
 * a single instance manages the configuration throughout the application's lifecycle.
 *
 * It provides functionality to load configuration from a file, generate a default configuration
 * if no configuration file exists, and retrieve the current configuration. The class uses
 * {@link Configuration}, which contains customizable settings, including port, upload paths,
 * forbidden file extensions, and endpoints definitions.
 */
public class ConfigurationManager {

    private static ConfigurationManager instance;
    private static Configuration config;

    private ConfigurationManager() {
        //loadConfigurationFromFile("config/config.json");
    }

    /**
     * Retrieves the singleton instance of the {@code ConfigurationManager} class.
     *
     * This method ensures that only one instance of the {@code ConfigurationManager}
     * is created, implementing the Singleton design pattern. If the instance has
     * not been initialized, it will create a new {@code ConfigurationManager} object.
     *
     * @return the singleton instance of {@code ConfigurationManager}
     */
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }

        return instance;
    }


    /**
     * Loads the application configuration from the specified file.
     *
     * This method reads a configuration file, parses it into JSON format,
     * and converts it into a {@link Configuration} object. If the configuration
     * file does not exist, it generates a default configuration instead.
     *
     * The method also initializes endpoint handlers specified in the configuration
     * if the handler class is provided for any endpoint.
     *
     * @param filePath the path to the configuration file to be loaded
     * @throws IOException if an error occurs while reading the configuration file
     */
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

        //set up a default endpoint that echoes the request back to the sender
        Endpoint ep = new Endpoint();
        ep.setPath("/echo");
        ep.setHandler("Handlers.EchoHandler");
        ep.setMethod("POST");
        conf.setEndpoints(Set.of(ep));

        String cfgFilePath = "config/config.json";
        try {
            //write configuration to file
            JsonNode cfgNode = Json.toJson(conf);
            String srcToWrite = Json.stringifyPretty(cfgNode);
            FileHandler.writeSystemFile(cfgFilePath, srcToWrite.getBytes());
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

    /**
     * Represents an exception that is thrown when there is an issue with the configuration
     * in the application. This is primarily used to signal errors encountered while processing,
     * reading, or generating configuration data.
     *
     * This exception extends {@link RuntimeException}, allowing it to be thrown without requiring
     * explicit handling unless explicitly desired. It can be used to wrap underlying causes of
     * configuration failures for easier debugging.
     *
     * The typical scenarios in which a {@code ConfigurationException} might be thrown include:
     * - Errors during configuration file reading or parsing.
     * - Failures in writing configuration data to a file.
     * - Invalid or corrupted configuration data detected during application initialization.
     */
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
