package Threads;
import java.util.LinkedList;

import Messages.*;
import Tools.ResponseBuilder;

/**
 * The ConsumerThread class extends the Thread class and is responsible for processing
 * requests from an input message queue and generating corresponding responses to be
 * placed onto an output message queue.
 *
 * This class continuously runs in a loop, retrieving requests from the input queue,
 * processing them, and placing the generated response into the output queue.
 * Thread-safe mechanisms are used to manage access to shared resources.
 */
public class ConsumerThread extends Thread {
    //shared resources: inputMessageQueue AND outputMessageQueue
    private final LinkedList<Request> inputMessageQueue;
    private final LinkedList<Response> outputMessageQueue;

    /**
     * Constructs a ConsumerThread instance that processes incoming requests from the input
     * message queue and produces corresponding responses to be placed in the output message queue.
     * The thread operates on shared resources, ensuring that access to the queues is thread-safe.
     *
     * @param inputMQ the shared LinkedList that holds incoming Request objects to be processed
     * @param outputMQ the shared LinkedList that receives Response objects created from processed requests
     */
    public ConsumerThread(LinkedList<Request> inputMQ, LinkedList<Response> outputMQ) {
        this.inputMessageQueue = inputMQ;
        this.outputMessageQueue = outputMQ;
    }

    /**
     * Continuously processes requests from the input message queue and generates corresponding
     * responses to be placed onto the output message queue.
     *
     * This method executes in an infinite loop. It waits for requests to become available
     * in the input message queue, processes each request to generate a response, and then
     * places the response into the output message queue. Both input and output queues are
     * synchronized to ensure thread-safe operations.
     *
     * The method adheres to the following steps:
     * 1. Waits for a request to be available in the input message queue if it is empty.
     * 2. Retrieves the next request from the queue.
     * 3. Logs an error and skips processing if the request is null.
     * 4. Uses the `ResponseBuilder` to create a corresponding response object from the request.
     * 5. Logs an error and skips processing if the response is null.
     * 6. Adds the generated response to the output message queue and notifies other threads
     *    waiting on the output queue.
     * 7. Handles any exceptions that may occur during the process by logging the exception
     *    message and interrupting the thread.
     *
     * The method relies on proper synchronization and notification mechanisms to ensure
     * a producer-consumer pattern and prevent deadlocks in a multithreaded environment.
     */
    @Override
    public void run() {
        while (true) {
            Request req = null;
            try {
                synchronized (inputMessageQueue) {
                    if (inputMessageQueue.isEmpty()) {
                        inputMessageQueue.wait();
                    }
                    req = inputMessageQueue.poll();
                }

                //defend against rogue nulls
                if (req == null) {
                    System.err.println("Consumer thread received null request!");
                    continue;
                }

                Response resp = ResponseBuilder.buildResponse(req);
                //push new response onto outputMessageQueue
               if (resp == null) {
                   System.err.println("Consumer thread received null response!");
                   continue;
               }

               synchronized (outputMessageQueue) {
                   outputMessageQueue.add(resp);
                   outputMessageQueue.notifyAll();
               }
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }
}
