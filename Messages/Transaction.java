package Messages;

public class Transaction {
    private Request req;
    private Response resp;
    private boolean complete = false;

    public Transaction(Request req, Response resp) {
        this.req = req;
        this.resp = resp;
    }

    public Transaction(Request req) {
        this.req = req;
        this.resp = null;
    }

    public Request getRequest() {
        return req;
    }
    public boolean isComplete() {
        return complete;
    }
    public void setComplete(boolean complete) {
        this.complete = complete;
    }
    public Response getResponse() {
        return resp;
    }
    public void setRequest(Request newReq) {
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
