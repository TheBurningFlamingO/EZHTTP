/**
 * @author Reilly McEntegart
 * @author Ivan Pedro Collado
 * @author David Leija
 * @author Raghav Sandeep Sharavanan
 * 
 * This file is a simple prototype of the HTTP server this project
 *  aims to create. This program is not representative of the final product,
 *  but a demonstration of its function and intended features. Since many of the
 *  features are not currently implemented, most features demonstrated in this file
 *  are simulated.
 * The HTTP version simulated here is HTTP/1.1 but the program may be extended to support
 *  HTTP/2.
 */
import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
public class HTToilerPaperServer {
    public static void main(String[] args) {
        int port = 8080;

        try {
            //bind server to a port with a socket
            ServerSocket socket = new ServerSocket(port);
            System.out.println("Server started and listening on port " + port + "." );

            //listen
            while (true) {
                try {
                    Socket client = socket.accept();
                    Request req = new Request(client);
                    
                }
                catch (IOException e) {
                    System.out.println("Error receiving connection.\n" + e.getMessage());
                } 

            }
        }
        catch (IOException e) {
            System.out.println("Fatal error involving socket on port " + port + ".\n" + e.getMessage());
            return;
        }

    }

    /* !This method was superceded by the constructor for the Request class!

     * @brief This method listens on a port for an incoming request and constructs an appropriate response
     * @apiNote **skeleton implementation**
     * @param socket, a (valid) socket to listen on
     * @throws IOException
     *
    private static void listen(Socket socket) throws IOException {
        //initialize an input stream, a reader for the input stream, and then read that stream into a buffer
        InputStream message = socket.getInputStream();
        InputStreamReader msgReader = new InputStreamReader(message);
        BufferedReader bufMsgReader = new BufferedReader(msgReader);

        //prepare output streams for response
        OutputStream response = socket.getOutputStream();
        PrintWriter writer = new PrintWriter(response, true);
        
        //output request to console
        System.out.println("Received message: " + bufMsgReader.readLine());

        //discard rest of buffer (processing the actual request is not yet implemented)
        while (bufMsgReader.readLine().length() != 0) {}
        //construct sign of life response.
        String hw = "Hello, world!";
        String responseText = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + hw.length() + "\r\n\r\n" + hw;
        System.out.println(responseText);

        //send response to client
        writer.print(responseText);
        writer.flush();
    }*/
}