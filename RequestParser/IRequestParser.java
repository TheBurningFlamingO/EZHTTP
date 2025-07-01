package RequestParser;
import java.io.IOException;
import java.net.Socket;
import Request.*;

public interface IRequestParser {
    static Request parse(Socket serverSocket) throws IOException {
        throw new UnsupportedOperationException("Not implemented!");
    }
}