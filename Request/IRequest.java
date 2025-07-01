package Request;
import java.util.HashMap;
public interface IRequest {
    String getMethod();
    String getPath();
    String getHTTPVersion();
    HashMap<String, String> getHeaders();
    String getBody();
}
