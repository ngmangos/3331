import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class OriginServerErrors {
    private static final String HOST = "127.0.0.1";
    private final int port;
    private final int timeout;
    private final ExecutorService threadPool;
    
    public OriginServerErrors(int port, int timeout) {
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
                String path = parts[1];
                
                // Read headers
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    // Skip headers for this simple implementation
                }

                System.out.println(path);
                
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
                    System.out.println("Not found");
                    sendErrorResponse(out, 404, "Not Found");
                } else if (path.equals("/slow")) {
                    // Send response slowly
                    out.print("HTTP/1.1 200 OK\r\n");
                    out.print("Content-Type: text/plain\r\n");
                    out.print("Content-Length: 13\r\n");
                    out.print("\r\n");
                    out.flush();
                    Thread.sleep(1000);
                    out.println("Slow response");
                    out.flush();
                } else if (path.equals("/chunked")) {
                    OutputStream rawOut = clientSocket.getOutputStream();
                    PrintWriter outWriter = new PrintWriter(rawOut, true);

                    outWriter.print("HTTP/1.1 200 OK\r\n");
                    outWriter.print("Content-Type: text/plain\r\n");
                    outWriter.print("Transfer-Encoding: chunked\r\n");
                    outWriter.print("\r\n");
                    outWriter.flush();

                    // Simulate sending 3 chunks
                    sendChunk(rawOut, "This is chunk one.");
                    Thread.sleep(500);
                    sendChunk(rawOut, "Second chunk coming.");
                    Thread.sleep(500);
                    sendChunk(rawOut, "Final chunk.");
                    
                    // End chunked stream
                    rawOut.write("0\r\n\r\n".getBytes());
                    rawOut.flush();
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
            out.print("HTTP/1.1 " + code + " " + reason + "\r\n");
            out.print("Content-Type: text/plain" + "\r\n");
            out.print("Content-Length: " + body.length() + "\r\n");
            out.print("\r\n");
            out.print(body);
        }
        
        private void sendErrorResponse(PrintWriter out, int code, String reason) {
            sendResponse(out, code, reason, "Error: " + reason);
        }

        private void sendChunk(OutputStream out, String data) throws IOException {
            String chunkSize = Integer.toHexString(data.length());
            out.write((chunkSize + "\r\n").getBytes());
            out.write((data + "\r\n").getBytes());
            out.flush();
        }
    }
    
    public static void main(String[] args) {
        
        int port = 8000;
        int timeout = 20000;
        
        OriginServerErrors server = new OriginServerErrors(port, timeout);
        server.start();
    }
}