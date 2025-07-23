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
    protected final LinkedList<Request> inputMessageQueue;
    protected final ServerSocket serverSocket;

    public AcceptorThread(LinkedList<Request> inputMessageQueue, ServerSocket serverSocket) {
        this.inputMessageQueue = inputMessageQueue;
        this.serverSocket = serverSocket;
    }

    public void run() {
        while (true) {
            try {
                Socket client = serverSocket.accept();
                Request req = RequestParser.parse(client);
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
