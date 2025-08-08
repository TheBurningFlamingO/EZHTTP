package Tools;

import Data.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.FileNotFoundException;
import java.io.IOException;

public class ConfigurationManager {

    private static ConfigurationManager instance;
    private static Configuration config;

    private ConfigurationManager() {

    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            instance = new ConfigurationManager();
        }

        return instance;
    }



    public void loadConfigurationFromFile(String filePath) throws IOException {
        try {
            String configSrc = FileHandler.readSystemFile(filePath);
            JsonNode cfgNode = Json.parse(configSrc);
            config = Json.fromJson(cfgNode, Configuration.class);
        }
        catch (FileNotFoundException e) {
            generateDefaultConfiguration();
        }
    }

    private void generateDefaultConfiguration() {
        Configuration conf = new Configuration();
        conf.setPort(8080);
        conf.setRootPath("webroot");
        conf.setUploadPath("upload");

        String cfgFilePath = "config/config.json";
        try {
            //write configuration to file
            JsonNode cfgNode = Json.toJson(conf);
            String srcToWrite = Json.stringify(cfgNode);
            FileHandler.writeSystemFile(cfgFilePath, srcToWrite);
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
     * Returns the current configuration
     */
    public Configuration getCurrentConfiguration() {
        if (config == null) {
            config = new Configuration();
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
