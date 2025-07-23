package Threads;
import java.util.HashMap;
import java.util.LinkedList;
import java.net.*;
import Messages.*;

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
            Request req;
            try {
                synchronized (inputMessageQueue) {
                    if (inputMessageQueue.isEmpty()) {
                        inputMessageQueue.wait();
                    }
                    req = inputMessageQueue.poll();
                }

                //produce canned response
                Socket client = req.getSocket();
                if (client == null)
                    throw new InterruptedException("Socket uninitialized.");

                //placeholder here; replace this code later with ResponseBuilder logic
                String httpVer = "HTTP/1.1";
                String responseCode = "200 OK";
                String body = req.toString();
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "text/plain");
                headers.put("Content-Length", String.valueOf(body.length()));

                Response resp = new Response(responseCode, httpVer, headers, body, client);

                //push new response onto outputMessageQueue
                synchronized (outputMessageQueue) {
                    outputMessageQueue.add(resp);
                    outputMessageQueue.notifyAll();
                }

            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
