import java.io.*;
import java.net.*;

public class OriginServerConnect {
    public static void main(String[] args) {
        int port = 443; // Origin server port

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Origin Server listening on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     InputStream in = clientSocket.getInputStream();
                     OutputStream out = clientSocket.getOutputStream()) {

                    // Read request (optional, for debugging)
                    byte[] buffer = new byte[1024];
                    int bytesRead = in.read(buffer);

                    String line = new String(buffer, 0, bytesRead);
                    if (line.isEmpty())
                        return;
                    
                    System.out.println("Received: " + line);

                    // Send a simple HTTP response
                    String response = "HTTP/1.1 200 OK\r\n" +
                                     "Content-Type: text/plain\r\n" +
                                     "Connection: close\r\n" +
                                     "\r\n" +
                                     "Hello from origin server";

                    out.write(response.getBytes());

                    bytesRead = in.read(buffer);

                    line = new String(buffer, 0, bytesRead);
                    if (line.isEmpty())
                        return;
                    
                    System.out.println("Received: " + line);
                    System.out.println("Server reached the end");
                    while (true) {}
                } catch (IOException e) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}