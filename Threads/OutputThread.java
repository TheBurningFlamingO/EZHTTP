package Threads;
import Messages.*;
import java.io.*;
import java.util.LinkedList;

public class OutputThread extends Thread {
    private final LinkedList<Response> outputMessageQueue;

    public OutputThread(LinkedList<Response> outputMQ) {
        this.outputMessageQueue = outputMQ;
    }
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
