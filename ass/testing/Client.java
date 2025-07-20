import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        String proxyHost = "127.0.0.1";
        int proxyPort = 8080; // Proxy's listening port
        String targetUrl = "http://localhost:8000/test"; // Request to forward via proxy

        try (Socket proxySocket = new Socket(proxyHost, proxyPort);
            PrintWriter out = new PrintWriter(proxySocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(proxySocket.getInputStream()))) {

            // Send HTTP GET request via proxy
            String request = "GET " + targetUrl + " HTTP/1.1\r\n" +
                            "Host: localhost:8000\r\n" +
                            "Connection: keep-alive\r\n" +
                            "\r\n";
            out.println(request);

            // Read response from proxy
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Response from proxy: " + line);
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}