import java.net.Socket;
import java.io.*;
public class Request {
    private String content = "";
    private int size = 0;


    /**
     * Reads an incoming request from a bound socket
     * Precondition: Request is active and non-empty
     * Postcondition: an object containing the message text and its size
     * @param socket (Socket) - The socket to read the message from
     */
    public Request(Socket socket) {
        try {
            //read input from socket
            InputStream is = socket.getInputStream();
            InputStreamReader isReader = new InputStreamReader(is);
            BufferedReader isBuf = new BufferedReader(isReader);
            
            //read incoming request content
            String line;
            while ((line = isBuf.readLine()) != null && !line.isEmpty()) {
                content += line;
            }
            size = content.length();
            //free readers
            isBuf.close();
            isReader.close();
            is.close();
        }
        catch (IOException e) {
            System.out.println("Error receiving input stream");
            System.out.println(e.getMessage());
        }
    }

    public String getContent() {
        return content;
    }
    public int getSize() {
        return size;
    }
    public byte[] getBytes() {
        return content.getBytes();
    }
}