package Messages;
public class Response extends Message {
    public Response(String method, String path, String httpVersion, HashMap<String, String> headers, String body,
    Socket socket) {
        super(method, path, httpVersion, headers, body, socket);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(httpVersion + " " + method + "\r\n");
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
}
