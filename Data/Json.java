package Data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

/**
 * Basic abstraction for JSON data
 * Uses Jackson to decode from and encode to JSON
 */
public class Json {

    private static final ObjectMapper mapper = defaultObjectMapper();

    /**
     * Creates and configures a default {@link ObjectMapper} instance.
     * The configured ObjectMapper is set to ignore unknown properties during deserialization,
     * ensuring greater flexibility when processing JSON data.
     *
     * @return a pre-configured {@link ObjectMapper} instance for JSON processing
     */
    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    /**
     * Parses a JSON string and converts it into a {@code JsonNode} object.
     * This method acts as a utility for converting JSON-formatted text
     * into a tree-like data structure for further manipulation or querying.
     *
     * @param jsonSrc the JSON-formatted string to be parsed
     * @return a {@code JsonNode} object representing the parsed JSON structure
     * @throws JsonProcessingException if an error occurs during JSON processing
     * @throws JsonMappingException if the content of the JSON cannot be mapped to a valid node
     */
    public static JsonNode parse(String jsonSrc) throws JsonProcessingException, JsonMappingException {
        return mapper.readTree(jsonSrc);
    }

    /**
     * Converts a {@code JsonNode} instance into an object of the specified class type.
     * This method uses the Jackson library to perform the conversion.
     *
     * @param <A> the type of the resulting object
     * @param jsonNode the {@code JsonNode} to convert
     * @param clazz the {@code Class} object representing the type to convert to
     * @return an instance of the specified type populated with data from the provided {@code JsonNode}
     * @throws JsonProcessingException if an error occurs during the conversion process
     */
    public static <A> A fromJson(JsonNode jsonNode, Class<A> clazz) throws JsonProcessingException {
        return mapper.treeToValue(jsonNode, clazz);
    }

    /**
     * Converts an object into a {@code JsonNode} representation.
     * This method uses Jackson's {@link ObjectMapper} to serialize the object
     * into a tree-like JSON structure.
     *
     * @param obj the object to be converted into a JSON tree
     * @return a {@code JsonNode} representing the JSON structure of the input object
     * @throws JsonProcessingException if an error occurs during the conversion process
     */
    public static JsonNode toJson(Object obj) throws JsonProcessingException {
        return mapper.valueToTree(obj);
    }

    /**
     * Converts an object into its JSON string representation.
     * The JSON string can optionally be formatted in a pretty-printed style.
     *
     * @param obj the object to be converted to JSON
     * @param pretty a boolean flag indicating whether the JSON should be pretty-printed or compact
     * @return the JSON string representation of the provided object
     * @throws JsonProcessingException if an error occurs during the JSON conversion process
     */
    private static String generateJson(Object obj, boolean pretty) throws JsonProcessingException {
        ObjectWriter ow = mapper.writer();

        if (pretty)
            return ow.withDefaultPrettyPrinter().writeValueAsString(obj);

        return ow.writeValueAsString(obj);
    }

    /**
     * Converts a {@code JsonNode} instance into a pretty-printed JSON string representation.
     * This method leverages the configured {@code ObjectMapper} to generate the JSON string.
     *
     * @param jsnNd the {@code JsonNode} to be converted to a JSON string
     * @return a formatted JSON string representation of the provided {@code JsonNode}
     * @throws JsonProcessingException if an error occurs during the JSON conversion process
     */
    public static String stringifyPretty(JsonNode jsnNd) throws JsonProcessingException {
        return generateJson(jsnNd, true);
    }

    /**
     * Converts a {@code JsonNode} object into its JSON string representation.
     * The JSON string is output in a compact, non-pretty-printed format.
     *
     * @param jsonNode the {@code JsonNode} instance to be converted into a JSON string
     * @return the compact JSON string representation of the provided {@code JsonNode}
     * @throws JsonProcessingException if any error occurs during the JSON processing,
     *                                 such as invalid or incompatible JSON structure
     */
    public static String stringify(JsonNode jsonNode) throws JsonProcessingException {
        return generateJson(jsonNode, false);
    }
}
