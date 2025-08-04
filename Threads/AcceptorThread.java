package Threads;
import Messages.*;
import Tools.*;
import java.util.LinkedList;
import java.io.IOException;
import java.net.*;

/**
 * This class accepts connections, parses requests, and pushes them onto the input message queue
 * Acceptor and producer thread
 */
public class AcceptorThread extends Thread {
    //shared resource: inputMQ
    protected final LinkedList<Request> inputMessageQueue;

    //the socket the server is using to listen for requests.
    protected final ServerSocket serverSocket;


    /**
     * Creates a new AcceptorThread
     * This thread accepts connections
     * There can only ever be one acceptor thread
     * @param inputMessageQueue The processing queue
     * @param serverSocket The socket the server is listening to
     */
    public AcceptorThread(LinkedList<Request> inputMessageQueue, ServerSocket serverSocket) {
        this.inputMessageQueue = inputMessageQueue;
        this.serverSocket = serverSocket;
    }

    /**
     * Continuously accepts incoming client connections, parses the requests, and adds them to
     * the input message queue for further processing. The thread listens on the server socket
     * and processes requests in a synchronized manner to ensure thread safety when interacting
     * with the shared input message queue.
     *
     * The method operates in an infinite loop and performs the following steps:
     * - Accepts an incoming client connection using the server socket.
     * - Parses the client's request to construct a {@link Request} object.
     * - Validates the parsed request and ensures it is not null before processing.
     * - Adds the request to the shared input message queue while holding a lock to prevent
     *   concurrent modifications by other threads.
     * - Notifies any waiting threads that new data has been added to the input message queue.
     *
     * If an {@link IOException} occurs while accepting client connections or reading the request,
     * the exception is caught and the error is logged to the standard error output. This ensures
     * that the thread continues to run and process later requests.
     *
     * This method is intended to be run in its own thread since it blocks indefinitely
     * on the server socket while waiting for incoming connections.
     */
    public void run() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                Request req = RequestParser.parse(client);

                //defend against rogue nulls
                if (req == null) continue;

                synchronized (inputMessageQueue) {
                    inputMessageQueue.add(req);
                    inputMessageQueue.notifyAll();
                }

            }
            catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

}
