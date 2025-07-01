package RequestParser;
import Request.*;
/**
 * Tool class; static methods only
 */
public class RequestParser implements IRequestParser {
    private static final String[] ACCEPTED_METHODS = {
        "GET",
        "POST"
    };
    //returns an explicit GET request or POST request, based on the message's content
    public static Request differentiateRequest(Request req) {
        String method = req.getMethod();
        for (String s: ACCEPTED_METHODS) {
            if (method.equals(s) && s.equals("GET")) {
                return new GETRequest(req);
            }
        }
    }


}