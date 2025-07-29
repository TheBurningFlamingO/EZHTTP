package Tools;
import Messages.*;
import java.util.ArrayList;
/*
 * A transaction is any interaction between the server and a client.
 * This class keeps a record of transactions that the server has processed,
 * including both accepted and discarded requests.
 */
public class TxnLogger {
    private static final ArrayList<Transaction> txns = new ArrayList<>();


    /**
     * Retrieves the Transaction object at the specified index from the transaction log.
     *
     * @param index the index of the requested transaction in the log; must be within
     *              the bounds of the transaction list (0 <= index < size of the list)
     * @return the Transaction object at the specified index
     * @throws ArrayIndexOutOfBoundsException if the provided index is out of bounds
     */
    public static Transaction getTransaction(int index) {
        if (index >= 0 && index < txns.size()) {
            return txns.get(index);
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    public static void logTransaction(Transaction txn) {
        if (txn != null) {
            txns.add(txn);
        }
    }

}
