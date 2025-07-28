package Tools;
import Messages.*;
import java.util.ArrayList;
/*
 * A transaction is any interaction between the server and a client.
 * This class keeps a record of transactions that the server has processed,
 * including both accepted and discarded requests.
 */
public class TxnLogger {
    ArrayList<Transaction> txns;

    public TxnLogger() {
        txns = new ArrayList<>();
    }

    /**
     *
     * @param index
     * @return
     */
    public Transaction getTransaction(int index) {
        if (index < txns.size()) {
            return txns.get(index);
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }
}
