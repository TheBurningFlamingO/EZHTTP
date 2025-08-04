/**
 * @file EZHTTPServer.java
 * @author Reilly McEntegart
 * @author Ivan Pedro Collado
 * @author David Leija
 * @author Raghav Sandeep Sharavanan
 *
 * 
 * This file is a simple prototype of the HTTP server this project
 *  aims to create. This program is not representative of the final product,
 *  but a demonstration of its function and intended features. Since many of the
 *  features are not currently implemented, most features demonstrated in this program
 *  are simulated.
 * The HTTP version simulated here is HTTP/1.1, but the program smay be extended to support
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

/**
 * The EZHTTPServer class is a simple HTTP server implementation that accepts client
 * connections, processes HTTP requests, and generates appropriate responses. The server
 * operates on a specified port and maintains the state of its operation using the
 * {@code ServerState} enum.
 *
 * Responsibilities of EZHTTPServer:
 * - Initializes the server to listen on a particular port.
 * - Manages the operational state of the server through the {@code getState} and {@code setState} methods.
 * - Spawns and starts three key threads for server operation:
 *   1. {@code AcceptorThread}: Handles client connections and parses incoming requests.
 *   2. {@code ConsumerThread}: Processes requests from the input queue and generates responses.
 *   3. {@code OutputThread}: Handles the sending of responses to clients through the output queue.
 *
 * The class operates as the main entry point for starting the server. It initializes
 * the necessary resources such as sockets, input and output queues, and server threads.
 * If an {@code IOException} occurs during server initialization, it logs the error
 * and transitions the server state to {@code CLOSED}.
 *
 * Key Features:
 * - Thread-based architecture for request handling, processing, and response delivery.
 * - Centralized server state management using the {@code ServerState} enum.
 */
public class EZHTTPServer {
    //start closed
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

            //begin threads
            acceptor.start();
            consumer.start();
            output.start();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            setState(ServerState.CLOSED);
        }
    }

    /**
     * Retrieves the current state of the server.
     *
     * @return the current state of the server as a {@code ServerState} enum value.
     */
    public static ServerState getState() {
        return state;
    }

    /**
     * Updates the current state of the server to the specified {@code ServerState}.
     * The state change is conditional; if the provided {@code newState} is null,
     * the state remains unchanged.
     *
     * @param newState the new state to set for the server. Must be a non-null
     *                 value of the {@code ServerState} enum.
     */
    public static void setState(ServerState newState) {
        if (newState == null) return;

        state = newState;
    }
}