import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String proxyHost = "127.0.0.1";
        int proxyPort = 8080; // Proxy's listening port
        String targetUrl = "http://localhost:8443/test";
        String connectUrl = "localhost:8443"; // Request to forward via proxy

        try (Socket proxySocket = new Socket(proxyHost, proxyPort);
            InputStream in = proxySocket.getInputStream();
            OutputStream out = proxySocket.getOutputStream()) {
            // Send HTTP GET request via proxy
            String request = "POST " + targetUrl + " HTTP/1.1\r\n" +
                            "Host: localhost:8443\r\n" +
                            "Connection: keep-alive\r\n" +
                            "Content-Length: 12\r\n" +
                            "\r\n" +
                            "HELLO HUMANS\n\n";
            String connect = "CONNECT " + connectUrl + " HTTP/1.1\r\n" +
                            "Host: localhost:8443\r\n" +
                            "\r\n";
            out.write(connect.getBytes());

            // Read response from proxy
            byte[] buffer = new byte[1024];

            out.write(request.getBytes());
            int bytesRead = in.read(buffer);
            String line = new String(buffer, 0, bytesRead);
            if (line.isEmpty())
                return;
            System.out.println(line);

            out.flush();
            out.write(request.getBytes());

            out.flush();
            out.write(request.getBytes());

            System.out.println("Client reached end");
            while (true) {}
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}