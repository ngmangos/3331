import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ClientErrors {
    private static final String PROXY_HOST = "127.0.0.1";
    private final int proxyPort;
    private final ExecutorService threadPool;
    
    public ClientErrors(int proxyPort) {
        this.proxyPort = proxyPort;
        this.threadPool = Executors.newCachedThreadPool();
    }
    
    public void testNormalRequest() {
        threadPool.execute(() -> sendRequest("GET http://example.com HTTP/1.1", "example.com"));
    }

    public void testCacheHit() {
        threadPool.execute(() -> sendRequest("GET http://example.com HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET http://example.com HTTP/1.1", "example.com"));
    }

    public void testCacheSynonyms() {
        threadPool.execute(() -> sendRequest("GET http://example.com HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET http://example.com/ HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET HTTP://EXAMPLE.COM/ HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET HTTP://EXAMPLE.COM:80/ HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET HTTP://EXAMPLE.COM:0080/ HTTP/1.1", "example.com"));
    }

    public void testCacheAntonyms() {
        threadPool.execute(() -> sendRequest("GET http://example.com/ HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET http://example.com/index.html HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET http://example.com/INDEX.HTML HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET http://example.com:8080/INDEX.HTML HTTP/1.1", "example.com"));
        threadPool.execute(() -> sendRequest("GET http://example.com:8080/INDEX.HTML?FOO=BAR HTTP/1.1", "example.com"));
    }

    public void testConcurrentPersistentConnections() {
        for (int i = 0; i < 3; i++) {
            final int clientId = i;
            threadPool.execute(() -> {
                try (Socket socket = new Socket(PROXY_HOST, proxyPort)) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    for (int j = 0; j < 3; j++) {
                        String path = "/persistent-" + j + "-" + clientId;
                        String requestLine = "GET http://localhost:8000" + path + " HTTP/1.1";

                        out.print(requestLine + "\r\n");
                        out.print("Host: localhost\r\n");
                        out.print("Connection: keep-alive\r\n");
                        out.print("\r\n");
                        out.flush();

                        System.out.println("Client " + clientId + ", request " + j + ": " + requestLine);

                        // Read response headers
                        String line;
                        int contentLength = -1;
                        while ((line = in.readLine()) != null) {
                            System.out.println("Client " + clientId + " <- " + line);
                            if (line.toLowerCase().startsWith("content-length:")) {
                                contentLength = Integer.parseInt(line.split(":")[1].trim());
                            }
                            if (line.isEmpty()) {
                                break;
                            }
                        }

                        // Read body (exactly contentLength bytes)
                        if (contentLength > 0) {
                            char[] body = new char[contentLength];
                            int read = 0;
                            while (read < contentLength) {
                                int r = in.read(body, read, contentLength - read);
                                if (r == -1) break;
                                read += r;
                            }
                            System.out.println("Client " + clientId + " <- " + new String(body));
                        } else {
                            System.out.println("Client " + clientId + " <- (No body or unknown length)");
                        }
                    }


                } catch (IOException e) {
                    System.out.println("Client " + clientId + " failed: " + e.getMessage());
                }
            });
        }
    }

    public void testCacheMiss() {
        threadPool.execute(() -> sendRequest("GET http://httpforever.com/ HTTP/1.1", "httpforever.com"));
        threadPool.execute(() -> sendRequest("GET http://httpforever.com/js/init.min.js HTTP/1.1", "httpforever.com"));
        threadPool.execute(() -> sendRequest("GET http://httpforever.com/js/init.min.js HTTP/1.1", "httpforever.com"));
        threadPool.execute(() -> sendRequest("GET http://httpforever.com/favicon.ico HTTP/1.1", "httpforever.com"));
    }
    
    public void testConnect() {
        threadPool.execute(() -> {
            try (Socket socket = new Socket(PROXY_HOST, proxyPort)) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Send CONNECT request
                out.print("CONNECT example.com:443 HTTP/1.1\r\n");
                out.print("Host: example.com:443\r\n");
                out.print("\r\n");
                out.flush();
                
                // Read response
                String line;
                System.out.println("CONNECT response: ");
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    if (line.isEmpty())
                        break;                        
                }
                
                // If we got here, CONNECT was successful
                System.out.println("CONNECT successful, can now tunnel HTTPS traffic");
                
            } catch (IOException e) {
                System.out.println("CONNECT failed: " + e.getMessage());
            }
        });
    }
    
    public void testMalformedConnect() {
        threadPool.execute(() -> sendRequest("CONNECT example.com:8000 HTTP/1.1", "example.com")); // Should fail (port not 443)
    }
    
    public void testNoHost() {
        threadPool.execute(() -> sendRequest("GET http:///test HTTP/1.1", "example.com")); // Missing host
    }
    
    public void testSelfLoop() {
        threadPool.execute(() -> sendRequest("GET http://" + PROXY_HOST + ":" + proxyPort + "/test HTTP/1.1", "localhost"));
    }
    
    public void testTimeout() {
        threadPool.execute(() -> sendRequest("GET http://localhost:8000/timeout HTTP/1.1", "localhost"));
    }
    
    public void testConnectionRefused() {
        threadPool.execute(() -> sendRequest("GET http://localhost:9999/refuse HTTP/1.1", "localhost")); // Assuming nothing on 9999
    }
    
    public void testConnectionClosed() {
        threadPool.execute(() -> sendRequest("GET http://localhost:8000/close HTTP/1.1", "localhost"));
    }

    public void testChunkedResponse() {
        threadPool.execute(() -> sendRequest("GET http://localhost:8000/chunked HTTP/1.1", "localhost"));
    }
    
    public void testConcurrentRequests(int count) {
        for (int i = 0; i < count; i++) {
            final int num = i;
            threadPool.execute(() -> {
                sendRequest("GET http://localhost:8000/concurrent-" + num + " HTTP/1.1", "localhost");
            });
        }
    }

    public void testDnsError() {
        threadPool.execute(() -> {
            // Use a domain that definitely doesn't exist
            sendRequest("GET http://nonexistentdomainthatshouldneverexist123.com/test HTTP/1.1", "example.com");
        });
    }
    
    private void sendRequest(String requestLine, String host) {
        try (Socket socket = new Socket(PROXY_HOST, proxyPort)) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send request
            out.print(requestLine + "\r\n");
            out.print("Host: " + host + "\r\n");
            out.print("Connection: keep-alive\r\n");
            out.print("\r\n");
            out.flush();
            
            // Read response
            String line;
            System.out.println("Response for '" + requestLine + "': ");
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Request failed for '" + requestLine + "': " + e.getMessage());
        }
    }
    
    public void shutdown() {
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java TestClient PROXY_PORT [TEST_CASE]");
            System.out.println("TEST_CASE can be: normal, connect, malformed, nohost, loop, timeout, refused, closed, concurrent");
            return;
        }
        
        int proxyPort = Integer.parseInt(args[0]);
        ClientErrors client = new ClientErrors(proxyPort);
        
        if (args.length > 1) {
            switch (args[1].toLowerCase()) {
                case "normal":
                    client.testNormalRequest();
                    break;
                case "cached":
                    client.testCacheHit();
                    break;
                case "synonyms":
                    client.testCacheSynonyms();
                    break;
                case "antonyms":
                    client.testCacheAntonyms();
                    break;
                case "miss":
                    client.testCacheMiss();
                    break;
                case "connect":
                    client.testConnect();
                    break;
                case "malformed":
                    client.testMalformedConnect();
                    break;
                case "nohost":
                    client.testNoHost();
                    break;
                case "loop":
                    client.testSelfLoop();
                    break;
                case "timeout":
                    client.testTimeout();
                    break;
                case "refused":
                    client.testConnectionRefused();
                    break;
                case "persist":
                    client.testConcurrentPersistentConnections();
                    break;
                case "closed":
                    client.testConnectionClosed();
                    break;
                case "concurrent":
                    int count = args.length > 2 ? Integer.parseInt(args[2]) : 10;
                    client.testConcurrentRequests(count);
                    break;
                case "chunked":
                    client.testChunkedResponse();
                    break;
                case "dns":
                    client.testDnsError();
                    break;
                default:
                    System.out.println("Unknown test case: " + args[1]);
            }
        } else {
            // Run all tests
            client.testNormalRequest();
            client.testConnect();
            client.testMalformedConnect();
            client.testNoHost();
            client.testSelfLoop();
            client.testTimeout();
            client.testConnectionRefused();
            client.testConnectionClosed();
            client.testConcurrentRequests(5);
        }
        
        // Wait for tests to complete
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        client.shutdown();
    }
}