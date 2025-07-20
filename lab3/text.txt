import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class WebServer {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: java WebServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        ServerSocket welcomeSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            new Thread(() -> handleClient(connectionSocket)).start(); // Optional: Handle each client in its own thread
        }
    }

    private static void handleClient(Socket connectionSocket) {
        try (
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream())
        ) {
            boolean keepAlive = true;

            while (keepAlive) {
                // Read request line
                String requestLine = inFromClient.readLine();
                if (requestLine == null || requestLine.isEmpty()) break;

                System.out.println("Request: " + requestLine);

                // Read remaining headers
                String headerLine;
                boolean clientWantsClose = false;
                while ((headerLine = inFromClient.readLine()) != null && !headerLine.isEmpty()) {
                    if (headerLine.toLowerCase().startsWith("connection: close")) {
                        clientWantsClose = true;
                    }
                }

                // Parse method and path
                String[] tokens = requestLine.split(" ");
                if (tokens.length < 2 || !tokens[0].equals("GET")) {
                    // Only support GET
                    outToClient.writeBytes("HTTP/1.1 501 Not Implemented\r\nConnection: close\r\n\r\n");
                    break;
                }

                String filename = tokens[1].equals("/") ? "index.html" : tokens[1].substring(1);
                File file = new File(filename);

                if (file.exists()) {
                    byte[] content = Files.readAllBytes(file.toPath());
                    outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
                    outToClient.writeBytes("Content-Type: " + getContentType(filename) + "\r\n");
                    outToClient.writeBytes("Content-Length: " + content.length + "\r\n");
                    outToClient.writeBytes("Connection: " + (clientWantsClose ? "close" : "keep-alive") + "\r\n");
                    outToClient.writeBytes("\r\n");
                    outToClient.write(content);
                } else {
                    String body = "<h1>404 Not Found</h1>";
                    byte[] bodyBytes = body.getBytes();
                    outToClient.writeBytes("HTTP/1.1 404 Not Found\r\n");
                    outToClient.writeBytes("Content-Type: text/html\r\n");
                    outToClient.writeBytes("Content-Length: " + bodyBytes.length + "\r\n");
                    outToClient.writeBytes("Connection: " + (clientWantsClose ? "close" : "keep-alive") + "\r\n");
                    outToClient.writeBytes("\r\n");
                    outToClient.write(bodyBytes);
                }

                outToClient.flush();
                if (clientWantsClose) break;
            }

            connectionSocket.close();

        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private static String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
