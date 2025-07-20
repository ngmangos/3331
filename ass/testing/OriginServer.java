import java.io.*;
import java.net.*;

public class OriginServer {
    public static void main(String[] args) {
        int port = 8000; // Origin server port

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Origin Server listening on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    // Read request (optional, for debugging)
                    String line;
                    while (!(line = in.readLine()).isEmpty()) {
                        System.out.println("Received: " + line);
                    }

                    // Send a simple HTTP response
                    String response = "HTTP/1.1 200 OK\r\n" +
                                     "Content-Type: text/plain\r\n" +
                                     "Connection: close\r\n" +
                                     "\r\n" +
                                     "Hello from Origin Server!";
                    out.println(response);

                } catch (IOException e) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}