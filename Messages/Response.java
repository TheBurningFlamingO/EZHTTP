package Messages;
import Tools.TxnLogger;

import java.util.*;
import java.net.Socket;
public class Response extends Message {
    private final String responseCode;

    public Response(String httpVersion, String responseCode, HashMap<String, String> headers, String body,
    Request origin) {
        super(httpVersion, headers, body, origin.getSocket());
        this.responseCode = responseCode;
        this.txn = origin.getTransaction();

        //update txn with response
        txn.setResponse(this);
        txn.setComplete(true);
        TxnLogger.logTransaction(txn);
    }
    
    public String toString() {
        String s = httpVersion + " " + responseCode + "\r\n" +
                //get the message tail (same for all messages)
                appendMessageTail();

        return s.trim();
    }
    public String getResponseCode() {
        return responseCode;
    }
}
