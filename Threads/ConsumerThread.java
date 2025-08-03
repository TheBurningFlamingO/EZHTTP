package Threads;
import java.util.HashMap;
import java.util.LinkedList;
import java.net.*;
import Messages.*;
import Tools.ResponseBuilder;

public class ConsumerThread extends Thread {
    //shared resource: inputMessageQueue
    private final LinkedList<Request> inputMessageQueue;
    private final LinkedList<Response> outputMessageQueue;

    public ConsumerThread(LinkedList<Request> inputMQ, LinkedList<Response> outputMQ) {
        this.inputMessageQueue = inputMQ;
        this.outputMessageQueue = outputMQ;
    }

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
