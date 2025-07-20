import java.util.Map;
import java.util.Hashtable;

public class Header {
    private Map<String, String> headers = new Hashtable<>();

    public Header(String[] lines) {
        for (String header : lines) {
            header = header.trim();
            headers.put(header.split(":")[0].toLowerCase(), header);
        }
    }

    public void updateHeader(String newHeader) {
        String[] headerParts = newHeader.split(":");
        if (headerParts.length < 2)
            return;
        headers.put(headerParts[0].toLowerCase(), newHeader);
    }

    public void removeHeader(String keyword) {
        headers.remove(keyword.toLowerCase());
    }

    public String getHeader(String keyword) {
        return headers.getOrDefault(keyword.toLowerCase(), "");
    }

    public boolean hasHeader(String keyword) {
        return headers.containsKey(keyword.toLowerCase());
    }

    public String getHeaderString() {
        String resultString = "";
        for (String value : headers.values()) 
            resultString += value + "\r\n";
        resultString += "\r\n";
        return resultString;
    }
}
