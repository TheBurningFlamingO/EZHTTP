package Tools;
import Messages.*;
import java.util.LinkedList;
import java.io.*;
/*
 * A transaction is any interaction between the server and a client.
 * This class keeps a record of transactions that the server has processed,
 * including both accepted and discarded requests.
 */
public class TxnLogger {
    private static final LinkedList<Transaction> txns = new LinkedList<>();
    private static final int MAX_SIZE = 1000;
    private static final String LOG_DIR = "txn_logs";
    private static final String LOG_FILE_NAME = "txn_log";

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

    /**
     * Logs a new transaction to the transaction history
     * @param txn the Transaction to log
     */
    public static void logTransaction(Transaction txn) {
        if (txn != null) {
            txns.add(txn);
        }
        //flush log if needed
        if (txns.size() > MAX_SIZE) {

        }
    }

    private void printLogs() {
        int suffix = 0;
        File logFile;
        do {
            suffix++;
            String filename = LOG_DIR + "/" + LOG_FILE_NAME + suffix;
            logFile = new File(filename);
        } while (logFile.exists());

        try (FileWriter fw = new FileWriter(logFile)) {
            while (!txns.isEmpty()) {
                Transaction txn = txns.poll();
                fw.write(txn.toString() + "\n");
            }
            fw.flush();
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }
}
