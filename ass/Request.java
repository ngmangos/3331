import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Request {
    private String requestType = "";
    private String destinationName = "";
    private int destinationPort = 80;
    private String file = "";
    private String connectionType = "HTTP/1.1";
    private Header header;
    private boolean empty = false;
    private boolean invalid = false;
    private String clientConnectionHeader = "Connection: close";
    private boolean connectionClose = false;
    private String messageBody = "";

    public String getRequestType() {
        return requestType;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public boolean isEmpty() {
        return empty;
    }

    public boolean connectionClose() {
        return connectionClose;
    }

    public String getClientConnectionHeader() {
        return clientConnectionHeader;
    }

    public boolean messageComplete() {
        if (header.hasHeader("transfer-encoding"))
            return false;
        if (!header.hasHeader("content-length"))
            return true;
        String headerString = header.getHeader("content-length");
        int contentLength = 0;
        try {
            contentLength = Integer.parseInt(headerString.split(":", 2)[1].trim());
        } catch (NumberFormatException e) {
            return true;
        }
        return messageBody.length() >= contentLength;
    }

    public void addToMessage(String messageContinued) {
        messageBody += messageContinued;
    }

    public String buildServerRequest() {
        String request = requestType + " " + destinationName + " " + connectionType + "\r\n" +
                        header.getHeaderString() + "\r\n" +
                        messageBody;
        return request;
    }

    public Request(String request) {
        String[] requestArray = request.split("\r\n\r\n", 2);
        if (requestArray.length != 2) {
            System.out.println("Received request with no empty line (\\r\\n\\r\\n) - waiting for next request...");
            empty = true;
            return;
        }

        String[] lines = requestArray[0].split("\r\n");
        messageBody = requestArray[1];

        String requestLine = lines.length > 0 ? lines[0].trim() : "";

        if (requestLine.isEmpty()) {
            System.out.println("Received empty request - waiting for next request...");
            empty = true;
            return;
        }
        System.out.println("Received: " + requestLine.substring(0, Math.min(20, requestLine.length())));

        String[] requestLineArray = requestLine.split(" ");
        requestType = requestLineArray[0];
        if (!Arrays.asList("GET", "HEAD", "POST", "CONNECT").stream().anyMatch(method -> method.equals(requestType))) {
            System.out.println("Not a valid request type.");
            invalid = true;
            return;
        }

        if (requestLineArray.length != 3) {
            System.out.println("Not enough request args.");
            invalid = true;
            return;
        }

        connectionType = requestLineArray[2];

        Pattern getPattern = Pattern.compile(requestType + "\\s+(.*)\\s+HTTP/1\\.1");
        Matcher matcher = getPattern.matcher(requestLine);

        if (!matcher.matches()) {
            System.out.println("Not a valid request line.");
            invalid = true;
            return;
        }

        header = new Header(Arrays.copyOfRange(lines, 1, lines.length));

        clientConnectionHeader = header.getHeader("Connection");
        connectionClose = header.getHeader("Proxy-Connection").toLowerCase().contains("close");
        connectionClose = header.getHeader("Connection").toLowerCase().contains("close");

        header.updateHeader("Via: 1.1 z5417382");
        header.updateHeader("Connection: close");
        header.removeHeader("Proxy-Connection");

        String requestTarget = requestLineArray[1];
        if (requestTarget.toLowerCase().startsWith("https://")) {
            requestTarget = requestTarget.substring(8);
        } else if (requestTarget.toLowerCase().startsWith("http://")) {
            requestTarget = requestTarget.substring(7);
        }

        destinationName = requestTarget.split("/")[0];
        file = requestTarget.substring(destinationName.length());
        if (!file.startsWith("/")) {
            file = "/" + file;
        }

        if (destinationName.contains(":")) {
            String[] hostNamesParts = destinationName.split(":");
            destinationName = hostNamesParts[0];
            destinationPort = Integer.parseInt(hostNamesParts[1]);
        }
    }
}
