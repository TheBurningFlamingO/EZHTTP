package RequestParser;
import Request.*;

public interface IRequestParser {
    static Request parse(BufferedReader bReader);
}
