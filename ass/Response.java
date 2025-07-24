import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Response {
    private ZonedDateTime responseDate = ZonedDateTime.now();
    private String requestType = "GET";
    private String connectionType = "HTTP/1.1";
    private int returnCode = 200;
    private String returnMessage = "OK";
    private Header header;
    private boolean invalid = false;
    private String messageBody = "";
    private int originServerPort = 80;
    private String originServerName;

    public int getContentLength() {
        return messageBody.length();
    }

    public String getRequestType() {
        return requestType;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public String getServerURL() {
        return originServerName + ":" + originServerPort;
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

    public boolean contentExpected() {
        if (returnCode < 200 || returnCode > 299)
            return true;
        return requestType.equals("POST") || requestType.equals("GET");
    }

    public void addToMessage(String messageContinued) {
        messageBody += messageContinued;
    }

    public String buildClientResponse() {
        String response = connectionType + " " + returnCode + " " + returnMessage + "\r\n" +
                        header.getHeaderString() + "\r\n" +
                        messageBody;
        return response;
    }
    
    public String getDateString() {
        return responseDate.format(DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z"));
    }

    public Response() {
        invalid = true;
    }

    public Response(String response, Request request) {
        String[] responseArray = response.split("\r\n\r\n", 2);
        if (responseArray.length != 2) {
            System.out.println("Received response with no empty line (\\r\\n\\r\\n) - waiting for next response...");
            invalid = true;
            return;
        }

        String[] lines = responseArray[0].split("\r\n");
        messageBody = responseArray[1];

        String responseLine = lines.length > 0 ? lines[0].trim() : "";

        if (responseLine.isEmpty()) {
            System.out.println("Received empty response - waiting for next response...");
            invalid = true;
            return;
        }
        System.out.println("Received: " + responseLine.substring(0, Math.min(20, responseLine.length())));

        String[] responseLineArray = responseLine.split(" ", 3);

        if (responseLineArray.length < 2) {
            System.out.println("Not enough response args.");
            invalid = true;
            return;
        }

        connectionType = responseLineArray[0];
        returnCode = Integer.parseInt(responseLineArray[1]);
        returnMessage = responseLineArray.length == 3 ? responseLineArray[2] : "";

        Pattern getPattern = Pattern.compile("HTTP/1\\.1\\s+(\\d{3})(.*)");
        Matcher matcher = getPattern.matcher(responseLine);

        if (!matcher.find()) {
            System.out.println("Not a valid response line.");
            invalid = true;
            return;
        }

        header = new Header(Arrays.copyOfRange(lines, 1, lines.length));

        header.updateHeader("Via: 1.1 z5417382");
        header.updateHeader(request.getClientConnectionHeader());
        requestType = request.getRequestType();
        originServerPort = request.getDestinationPort();
        originServerName = request.getDestinationName();
    }
}
