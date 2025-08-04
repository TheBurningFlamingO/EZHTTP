package Tools;
import Messages.*;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.io.*;
/*
 * A transaction is any interaction between the server and a client.
 * This class keeps a record of transactions that the server has processed,
 * including both accepted and discarded requests.
 */
public class TxnLogger {
    //transaction list
    private static final LinkedList<Transaction> txns = new LinkedList<>();
    //maximum size
    private static final int MAX_SIZE = 1000;
    //the directory to store logs
    private static final String LOG_DIR = "txn_logs";
    //the base filename of the log files
    private static final String LOG_FILE_NAME = "txn_log";

    //always verify on startup that the log directory exists
    static {
        //ensure log directory exists
        File logDir = new File(LOG_DIR);
        if (!logDir.mkdirs() && !logDir.exists()) {
            System.err.println();
            /* -server states should replace these exit statements*/
            System.exit(1);
        }
    }

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
     * Checks whether the transaction is currently stored within the log buffer
     * @param txn The queried transaction
     * @return true if the transaction exists in the queue; false otherwise
     */
    public static boolean isLogged(Transaction txn) {
        if (txns.isEmpty() || txn == null) return false;
        for (Transaction t : txns) {
            if (t.equals(txn)) return true;
        }

        return false;
    }

    /**
     * Logs a new transaction in the transaction history
     *
     * @param txn the Transaction to log
     */
    synchronized public static void logTransaction(Transaction txn) {
        if (txn != null && txn.isComplete()) {
            txns.add(txn);
        }
        //flush log if needed
        if (txns.size() > MAX_SIZE) {
            flushLogs();
        }
    }

    /**
     * Flushes the log buffer; triggers upon breach of maximum size
     * Writes the full contents of the buffer to a log file and then clears the buffer.
     */
    private static void flushLogs() {
        //Due to the logic of when this method is called, this branch is unlikely.
        //  However, in the name of a robust system, we must cover all bases.
        if (txns.isEmpty())
            return;

        //get current time and file name
        String timestamp = LocalDateTime.now().toString();
        String filename = String.format("%s/%s_%s.log", LOG_DIR, LOG_FILE_NAME, timestamp);

        //write to file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (Transaction txn : txns) {
                writer.write(txn.toString());
                writer.newLine();
            }
            writer.flush();
            txns.clear();
        }
        catch (IOException e) {
            System.err.println("Failed to write transaction log: " + e.getMessage());
        }
        finally {
            System.out.println("Transaction log flushed!");
        }
    }
}