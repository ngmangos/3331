import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.regex.*;

public class WebServer {
    private static final int KEEP_ALIVE_TIMEOUT = 20000; // 20 seconds in milliseconds
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java WebServer PORT");
            System.exit(1);
        }
        
        String host = "127.0.0.1";
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            System.exit(1);
            return;
        }
        
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(host, port));
            System.out.println("Server listening on " + host + ":" + port);
            
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Connected by " + clientSocket.getRemoteSocketAddress());
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private static void handleClient(Socket clientSocket) throws IOException {
        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {
            
            boolean keepAlive = true;
            
            while (keepAlive) {
                try {
                    clientSocket.setSoTimeout(KEEP_ALIVE_TIMEOUT);
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    
                    if (bytesRead == -1) {
                        System.out.println("Client closed connection.");
                        break;
                    }
                    
                    if (bytesRead == 0) {
                        continue;
                    }
                    
                    String request = new String(buffer, 0, bytesRead);
                    String[] lines = request.split("\r\n");
                    String requestLine = lines.length > 0 ? lines[0].trim() : "";
                    

                    if (requestLine.isEmpty()) {
                        System.out.println("Received empty request - waiting for next request...");
                        continue;
                    }
                    
                    System.out.println("Received: " + requestLine.substring(0, Math.min(20, requestLine.length())));
                    
                    // Parse the GET request
                    Pattern getPattern = Pattern.compile("GET (.*) HTTP/1\\.1");
                    Matcher matcher = getPattern.matcher(requestLine);
                    
                    if (!matcher.matches()) {
                        System.out.println("Not a valid GET request.");
                        break;
                    }
                    
                    String requestedFile = matcher.group(1);
                    if (requestedFile.startsWith("/")) {
                        requestedFile = requestedFile.substring(1);
                    }
                    
                    if (requestedFile.isEmpty()) {
                        requestedFile = "index.html";
                    }
                    
                    // Handle the request
                    handleHttpRequest(requestedFile, outputStream);
                    
                    // Check if client requested connection close
                    if (request.toLowerCase().contains("connection: close")) {
                        keepAlive = false;
                        System.out.println("Client requested connection close.");
                    }
                    
                } catch (SocketTimeoutException e) {
                    System.out.println("Keep-alive timeout (" + (KEEP_ALIVE_TIMEOUT/1000) + "s) reached - closing connection.");
                    break;
                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                    break;
                }
            }
        }
    }
    
    private static void handleHttpRequest(String fileName, OutputStream outputStream) throws IOException {
        String status;
        byte[] content;
        String contentType = "text/html";
        
        Path filePath = Paths.get(fileName);
        
        if (!Files.exists(filePath)) {
            // 404 Not Found
            System.out.println("File " + fileName + " not found.");
            status = "404 Not Found";
            content = "Page Not Found!".getBytes();
        } else {
            // 200 OK
            status = "200 OK";
            content = Files.readAllBytes(filePath);
            
            // Determine content type based on file extension
            String fileExtension = getFileExtension(fileName).toLowerCase();
            if (fileExtension.equals("html")) {
                contentType = "text/html";
            } else if (fileExtension.equals("jpeg") || fileExtension.equals("jpg")) {
                contentType = "image/jpeg";
            } else {
                contentType = "text/html";
            }
        }
        
        // Create HTTP response
        StringBuilder responseBuilder = new StringBuilder();
        responseBuilder.append("HTTP/1.1 ").append(status).append("\r\n");
        responseBuilder.append("Content-Length: ").append(content.length).append("\r\n");
        responseBuilder.append("Content-Type: ").append(contentType).append("\r\n");
        responseBuilder.append("Connection: keep-alive\r\n");
        responseBuilder.append("Keep-Alive: timeout=").append(KEEP_ALIVE_TIMEOUT / 1000).append(", max=100\r\n");
        responseBuilder.append("\r\n");
        
        // Send response
        outputStream.write(responseBuilder.toString().getBytes());
        outputStream.write(content);
        outputStream.flush();
        
        System.out.println("Sent response: " + status + " for " + fileName);
    }
    
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}