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
            Response response;
            try {
                synchronized (outputMessageQueue) {
                    while (outputMessageQueue.isEmpty()) {
                        outputMessageQueue.wait();
                    }
                }
                response = outputMessageQueue.poll();

            //construct and send response
            PrintWriter pw = new PrintWriter(response.getSocket().getOutputStream());
            pw.print(response);
            pw.flush();
            pw.close();

            //close connection
            response.getSocket().close();
        }
            catch (InterruptedException e) {
                System.out.println("Output thread Interrupted!");
                //force exit
                System.exit(e.getStackTrace().length);
            }
            catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(e.getStackTrace().length);
            }
        }
    }
}
