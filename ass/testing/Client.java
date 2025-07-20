import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String proxyHost = "127.0.0.1";
        int proxyPort = 8080; // Proxy's listening port
        String targetUrl = "http://localhost:8000/test"; // Request to forward via proxy

        try (Socket proxySocket = new Socket(proxyHost, proxyPort);
            InputStream in = proxySocket.getInputStream();
            OutputStream out = proxySocket.getOutputStream()) {
            // Send HTTP GET request via proxy
            String request = "POST " + targetUrl + " HTTP/1.1\r\n" +
                            "Host: localhost:8000\r\n" +
                            "Connection: keep-alive\r\n" +
                            "Content-Length: 12\r\n" +
                            "\r\n" +
                            "HELLO HUMANS";
            out.write(request.getBytes());

            // Read response from proxy
            byte[] buffer = new byte[1024];

            int bytesRead = in.read(buffer);
            String line = new String(buffer, 0, bytesRead);
            if (line.isEmpty())
                return;
            System.out.println(line);

            out.flush();

            out.write(request.getBytes());
            bytesRead = in.read(buffer);
            line = new String(buffer, 0, bytesRead);
            if (line.isEmpty())
                return;
            System.out.println(line);

            out.flush();

            out.write(request.getBytes());
            bytesRead = in.read(buffer);
            line = new String(buffer, 0, bytesRead);
            if (line.isEmpty())
                return;
            System.out.println(line);
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}