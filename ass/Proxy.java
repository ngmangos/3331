import java.io.*;
import java.net.*;

public class Proxy {
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
        try (InputStream clientInputStream = clientSocket.getInputStream();
             OutputStream clientOutputStream = clientSocket.getOutputStream()) {
            
            boolean keepAlive = true;
            
            while (keepAlive) {
                System.out.println("STARTING ONE");
                try {
                    clientSocket.setSoTimeout(KEEP_ALIVE_TIMEOUT);
                    byte[] buffer = new byte[1024];
                    int bytesRead = clientInputStream.read(buffer);
                    
                    if (bytesRead == -1) {
                        System.out.println("Client closed connection.");
                        break;
                    }
                    
                    if (bytesRead == 0) {
                        continue;
                    }
                    
                    String requestString = new String(buffer, 0, bytesRead);
                    Request request = new Request(requestString);

                    if (request.isEmpty()) {
                        continue;
                    } else if (request.isInvalid()) {
                        break;
                    }

                    // do i need more, get more
                    while (!request.messageComplete())  {
                        System.out.println("Need more message dude");
                        bytesRead = clientInputStream.read(buffer);
                        if (bytesRead == -1 || bytesRead == 0)
                            break;
                        requestString = new String(buffer, 0, bytesRead);
                        request.addToMessage(requestString);
                    }

                    Response response;
                    try (Socket originServerSocket = new Socket(request.getDestinationName(), request.getDestinationPort())) {
                        response = handleOrigin(originServerSocket, clientOutputStream, request);           
                    }

                    System.out.println(response.buildClientResponse());
                    // send response to client :)
                    clientOutputStream.write(response.buildClientResponse().getBytes());
                    clientOutputStream.flush();

                    if (request.connectionClose()) {
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
    
    private static Response handleOrigin(Socket originServerSocket, OutputStream clientOutputStream, Request request) {
        try (InputStream originInputStream = originServerSocket.getInputStream();
            OutputStream originOutputStream = originServerSocket.getOutputStream()
        ) {
            originOutputStream.write(request.buildServerRequest().getBytes());
            originOutputStream.flush();

            byte[] buffer = new byte[1024];
            int bytesRead = originInputStream.read(buffer);

            if (bytesRead == -1 || bytesRead == 0)
                return null;
            
            String responseString = new String(buffer, 0, bytesRead);
            Response response = new Response(responseString, request);
            
            if (response.isInvalid())
                return null;

            while (!response.messageComplete())  {
                System.out.println("Need more mannnn");
                bytesRead = originInputStream.read(buffer);
                if (bytesRead == -1 || bytesRead == 0)
                    break;
                responseString = new String(buffer, 0, bytesRead);
                response.addToMessage(responseString);
            }
            
            return response;
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            return new Response();
        }
    }
}