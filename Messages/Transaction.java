package Messages;

/**
 * The Transaction class represents a unit of work encompassing a request and its associated response.
 * It provides mechanisms for tracking request-response pairs and their completion status.
 */
public class Transaction {
    private Request req;
    private Response resp;
    private boolean complete = false;

    /**
     * Constructs a new, completed Transaction
     * @param req The request
     * @param resp The associated Response
     */
    public Transaction(Request req, Response resp) {
        this.req = req;
        this.resp = resp;
        this.complete = true;
    }

    /**
     * Constructs a new Transaction with the specified request.
     * The transaction is not considered complete until a response is associated with it.
     *
     * @param req The request associated with this transaction
     */
    public Transaction(Request req) {
        this.req = req;
        this.resp = null;
    }

    /**
     * Retrieves the request associated with this transaction.
     *
     * @return the Request object associated with this transaction
     */
    public Request getRequest() {
        return req;
    }

    /**
     * Checks whether a transaction is complete. A transaction is considered complete
     * when it has both a request and a response associated with it.
     *
     * @return true if the transaction is complete, false otherwise
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Sets the completion status of the transaction.
     *
     * @param complete a boolean value indicating whether the transaction is complete (true)
     *                 or not complete (false)
     */
    public void setComplete(boolean complete) {
        //a transaction should be immutable once complete
        //also should not be erroneously marked as such
        if (this.complete || getRequest() == null || getResponse() == null)
            return;

        this.complete = complete;
    }

    /**
     * Retrieves the response associated with this transaction.
     *
     * @return the Response object associated with this transaction, or null if no response is set
     */
    public Response getResponse() {
        return resp;
    }

    /**
     * Updates the request associated with this transaction.
     * Throws an IllegalArgumentException if the provided request is null.
     *
     * @param newReq the new Request object to associate with this transaction; must not be null*/
    public void setRequest(Request newReq) {
        //transactions are immutable once completed
        if (complete) return;

        if (newReq != null) {
            req = newReq;
            return;
        }
        throw new IllegalArgumentException("Request cannot be null!");
    }

    public void setResponse(Response newResp) {
        if (newResp != null) {
            resp = newResp;
            return;
        }
        throw new IllegalArgumentException("Response cannot be null!");
    }
}
