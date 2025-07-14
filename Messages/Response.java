package Messages;
public class Response extends Message {
    private String responseCode;
    public Response(String responseCode, String httpVersion, HashMap<String, String> headers, String body,
    Socket socket) {
        super(httpVersion, headers, body, socket);
        this.responseCode = responseCode;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(httpVersion + " " + responseCode + "\r\n");
        //header
        for (Map.Entry<String, String> header : headers.entrySet()) {
            sb.append(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        //end of header
        sb.append("\r\n");
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }

        return sb.toString().trim();
    }
    public String getResponseCode() {
        return responseCode;
    }
}
