import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Response {
    private String expectedType = "";
    private String connectionType = "HTTP/1.1";
    private String returnCode = "200";
    private String returnMessage = "OK";
    private Header header;
    private boolean invalid = false;
    private String messageBody = "";

    public boolean isInvalid() {
        return invalid;
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

    public String buildClientResponse() {
        String response = connectionType + " " + returnCode + " " + returnMessage + "\r\n" +
                        header.getHeaderString() + "\r\n" +
                        messageBody;
        return response;
    }

    public Response() {
        invalid = true;
    }

    public Response(String response) {
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

        if (responseLineArray.length != 3) {
            System.out.println("Not enough response args.");
            invalid = true;
            return;
        }

        connectionType = responseLineArray[2];

        Pattern getPattern = Pattern.compile("HTTP/1\\.1  ([0-9]+) (*)");
        Matcher matcher = getPattern.matcher(responseLine);

        if (!matcher.matches()) {
            System.out.println("Not a valid response line.");
            invalid = true;
            return;
        }

        header = new Header(Arrays.copyOfRange(lines, 1, lines.length));

        header.updateHeader("Via: 1.1 z5417382");
        header.updateHeader("Connection: close");
        header.removeHeader("Proxy-Connection");
    }
}
