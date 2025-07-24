/**
 * @author Reilly McEntegart
 * @author Ivan Pedro Collado
 * @author David Leija
 * @author Raghav Sandeep Sharavanan
 * 
 * This file is a simple prototype of the HTTP server this project
 *  aims to create. This program is not representative of the final product,
 *  but a demonstration of its function and intended features. Since many of the
 *  features are not currently implemented, most features demonstrated in this program
 *  are simulated.
 * The HTTP version simulated here is HTTP/1.1, but the program may be extended to support
 *  HTTP/2.
 *  test
 *
 *  TODO Security Improvements:
 *     - Implement HTTPS (optional)
 *     - Add authentication
 *     - Add rate limiting
 *     - Implement thread pooling
 *     - Add comprehensive input validation
 *
 *  TODO Critical Systems:
 *      -Security Validation
 *      -POST Request Processing
 *      -Exception Handling
 *      -Logging
 */
import java.io.*;
import java.net.ServerSocket;
import java.util.LinkedList;
import Data.*;
import Messages.*;
import Threads.*;

public class EZHTTPServer {
    private static ServerState state = ServerState.CLOSED;
    public static void main(String[] args) {
        int port = 8080;
        LinkedList<Request> inputMessageQueue = new LinkedList<>();
        LinkedList<Response> outputMessageQueue = new LinkedList<>();
        try {
            //bind server to a port with a socket
            ServerSocket socket = new ServerSocket(port);
            System.out.println("Server started and listening on port " + port + "." );
            //update server state
            state = ServerState.OPEN;

            //initialize threads
            AcceptorThread acceptor = new AcceptorThread(inputMessageQueue, socket);
            ConsumerThread consumer = new ConsumerThread(inputMessageQueue, outputMessageQueue);
            OutputThread output = new OutputThread(outputMessageQueue);
            acceptor.start();
            consumer.start();
            output.start();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            setState(ServerState.CLOSED);
        }
    }
    public static ServerState getState() {
        return state;
    }
    public static void setState(ServerState newState) {
        if (newState == null) return;

        state = newState;
    }
}