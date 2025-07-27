import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TestOriginServer {
    private static final String HOST = "127.0.0.1";
    private final int port;
    private final int timeout;
    private final ExecutorService threadPool;
    
    public TestOriginServer(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;
        this.threadPool = Executors.newCachedThreadPool();
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(HOST))) {
            System.out.println("Origin server listening on " + HOST + ":" + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new OriginHandler(clientSocket, timeout));
            }
        } catch (IOException e) {
            System.err.println("Origin server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
    
    private static class OriginHandler implements Runnable {
        private final Socket clientSocket;
        private final int timeout;
        
        public OriginHandler(Socket socket, int timeout) {
            this.clientSocket = socket;
            this.timeout = timeout;
        }
        
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                clientSocket.setSoTimeout(timeout);
                
                String requestLine = in.readLine();
                if (requestLine == null) return;
                
                String[] parts = requestLine.split(" ");
                if (parts.length < 3) {
                    sendErrorResponse(out, 400, "Bad Request");
                    return;
                }
                
                String method = parts[0];
                String path = parts[1];
                String version = parts[2];
                
                // Read headers
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    // Skip headers for this simple implementation
                }
                
                // Handle different test cases based on path
                if (path.equals("/timeout")) {
                    // Simulate timeout by sleeping longer than proxy timeout
                    Thread.sleep(timeout * 2);
                    sendResponse(out, 200, "OK", "Timeout test response");
                } else if (path.equals("/close")) {
                    // Close connection unexpectedly
                    clientSocket.close();
                    return;
                } else if (path.equals("/refuse")) {
                    // Simulate connection refused (not actually possible here)
                    sendErrorResponse(out, 502, "Connection refused");
                } else if (path.equals("/notfound")) {
                    sendErrorResponse(out, 404, "Not Found");
                } else if (path.equals("/slow")) {
                    // Send response slowly
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");
                    out.println("Content-Length: 13");
                    out.println();
                    out.flush();
                    Thread.sleep(1000);
                    out.println("Slow response");
                    out.flush();
                } else {
                    // Normal response
                    sendResponse(out, 200, "OK", "Normal response from origin");
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Origin server: Client timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                System.out.println("Origin server: Client connection error - " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        private void sendResponse(PrintWriter out, int code, String reason, String body) {
            out.println("HTTP/1.1 " + code + " " + reason);
            out.println("Content-Type: text/plain");
            out.println("Content-Length: " + body.length());
            out.println();
            out.println(body);
            out.flush();
        }
        
        private void sendErrorResponse(PrintWriter out, int code, String reason) {
            sendResponse(out, code, reason, "Error: " + reason);
        }
    }
    
    public static void main(String[] args) {
        
        int port = 8000;
        int timeout = 10000;
        
        TestOriginServer server = new TestOriginServer(port, timeout);
        server.start();
    }
}