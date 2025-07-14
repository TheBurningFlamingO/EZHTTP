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
import java.util.ArrayList;
import Messages.*;
import Tools.*;
public class HTToilerPaperServer {
    public static void main(String[] args) {
        int port = 8080;
        ArrayList<Request> inputMessageQueue = new ArrayList<>();
        try {
            //bind server to a port with a socket
            ServerSocket socket = new ServerSocket(port);
            System.out.println("Server started and listening on port " + port + "." );

            //listen
            while (true) {
                //listen on socket for an incoming message
                Socket client = socket.accept();
                new Thread(() -> {
                    try {
                        //socket.accept() hangs until a client connects, where we instantiate the request
                        Request req = RequestParser.parse(client);

                        //add the request to the message queue
                        inputMessageQueue.add(req);
                        System.out.println(inputMessageQueue.get(0));
                        PrintWriter pw = new PrintWriter(client.getOutputStream());
                        String body = req.toString();
                        String response = "HTTP/1.1 200 OK\r\n" +
                                          "Content-Type: text/plain\r\n" + 
                                          "Content-Length: " + body.length() + "\r\n\r\n" + body;
                        pw.print(response);
                        pw.flush();
                        pw.close();
                        client.close();
                    }
                    catch (IOException e) {
                        System.out.println("Error intercepting message request.\n" + e.getMessage());
                        return;
                    }
                }).start();
                
                
            }
        } catch (IOException e) {
            System.out.println("Error receiving connection\n" + e.getMessage());
            return;
        }
    }
}