package Data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

/**
 * Basic abstraction for JSON data
 * Uses Jackson to decode from and encode to JSON
 */
public class Json {
    private static final ObjectMapper mapper = defaultObjectMapper();

    public static ObjectMapper defaultObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public static JsonNode parse(String jsonSrc) throws JsonProcessingException, JsonMappingException {
        return mapper.readTree(jsonSrc);
    }

    public static <A> A fromJson(JsonNode jsonNode, Class<A> clazz) throws JsonProcessingException {
        return mapper.treeToValue(jsonNode, clazz);
    }

    public static JsonNode toJson(Object obj) throws JsonProcessingException {
        return mapper.valueToTree(obj);
    }

    private static String generateJson(Object obj, boolean pretty) throws JsonProcessingException {
        ObjectWriter ow = mapper.writer();

        if (pretty)
            return ow.withDefaultPrettyPrinter().writeValueAsString(obj);

        return ow.writeValueAsString(obj);
    }

    public static String stringifyPretty(JsonNode jsnNd) throws JsonProcessingException {
        return generateJson(jsnNd, true);
    }

    public static String stringify(JsonNode jsonNode) throws JsonProcessingException {
        return generateJson(jsonNode, false);
    }
}
