package Threads;
import Messages.*;
import java.io.*;
import java.util.LinkedList;

/**
 * The OutputThread class is responsible for processing and sending HTTP response messages
 * from the output message queue to the respective client connections. It continuously
 * retrieves responses from the shared {@link LinkedList} output message queue, formats
 * them, and writes them to the output stream of the associated client socket.
 *
 * The key functionality of this thread includes:
 * - Waiting for responses to be added to the output message queue.
 * - Handling synchronization to ensure thread-safe access to the shared queue.
 * - Constructing and sending HTTP responses to client sockets.
 * - Closing connections after the response has been successfully sent.
 *
 * The thread operates in an infinite loop and performs the following steps:
 * - Waits for the output message queue to contain data by calling {@link Object#wait()}
 *   on the queue when it is empty.
 * - Retrieves and removes a {@link Response} object from the queue for processing.
 * - Verifies the integrity of the {@link Response} object (e.g., non-null socket, non-null
 *   response string) before attempting to send it.
 * - Writes the HTTP response as a string to the corresponding client socket's output stream.
 * - Handles various exceptions that may occur during processing, such as interrupted
 *   exceptions, I/O exceptions, or unexpected runtime exceptions.
 * - Interrupts the thread and terminates gracefully if interrupted while waiting or processing.
 *
 * This class is designed to work independently in its own thread, ensuring asynchronous
 * processing of HTTP responses while managing shared resources with proper synchronization.
 */
public class OutputThread extends Thread {
    //!Shared resource: outputMessageQueue!
    private final LinkedList<Response> outputMessageQueue;

    /**
     * Constructs an OutputThread instance that processes responses from the shared output
     * message queue. This thread is responsible for consuming the responses and performing
     * any necessary operations, such as sending them to their intended destination or performing
     * additional processing.
     *
     * @param outputMQ the shared LinkedList that holds Response objects to be processed
     */
    public OutputThread(LinkedList<Response> outputMQ) {
        this.outputMessageQueue = outputMQ;
    }

    /**
     * Continuously processes {@link Response} objects from the shared output message queue
     * and sends them to their intended destinations via their associated sockets. This method
     * runs in an infinite loop and performs the following tasks:
     *
     * - Waits for the availability of responses in the output message queue.
     * - Retrieves a {@link Response} object from the queue in a synchronized manner.
     * - Handles potential null values for both the retrieved response and its associated socket.
     * - Constructs the response string using {@link Response#toString()}, and writes it
     *   to the output stream of the socket.
     * - Closes the socket connection after sending the response.
     *
     * Any exceptions during execution, including {@link InterruptedException} for thread interruption,
     * {@link IOException} for I/O issues, or other unexpected exceptions, are caught and logged.
     * In the case of an {@link InterruptedException}, the thread is interrupted and terminated.
     *
     * This method ensures proper synchronization when accessing the shared queue and
     * handles errors gracefully to maintain robust operation during message processing.
     */
    @Override
    public void run() {
        while (true) {
            Response response = null;
            try {
                synchronized (outputMessageQueue) {
                    while (outputMessageQueue.isEmpty()) {
                        outputMessageQueue.wait();
                    }
                    response = outputMessageQueue.poll();
                }
                if (response == null) {
                    System.err.println("Polled null response from outputMessageQueue!");
                    continue;
                }

                if (response.getSocket() == null) {
                    System.err.println("Response socket is null!");
                    continue;
                }

                //prepare a writer and write the response
                try (PrintWriter pw = new PrintWriter(response.getSocket().getOutputStream())) {
                    String responseStr = response.toString();
                    if (responseStr == null) {
                        System.err.println("Response string is null!");
                        continue;
                    }

                    //construct and send response
                    pw.print(responseStr);
                    pw.flush();
                }

                //close connection
                response.getSocket().close();
        }
            catch (InterruptedException e) {
                System.err.println("Output thread Interrupted: " + e.getMessage());
                //force exit
                Thread.currentThread().interrupt();
                break;
            }
            catch (IOException e) {
                System.err.println("IO Error in OutputThread: " + e.getMessage());
                e.printStackTrace();
            }
            catch (Exception e) {
                System.err.println("Unexpected exception in OutputThread: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
