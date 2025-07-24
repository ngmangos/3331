import java.io.*;
import java.net.*;
// Conditions:
// No body: GET req, HEAD, Connect -> Do not perform loop
// Body: GET res, POST
// If 'Transfer-encoding' present -> wait until 0 or -1 returned
// If 'Content-length' present
// In java implementation for InputStream, 0 return for InputStream.read(byte[1024]) is unpredicted
//      outputStream.write("".getBytes()) will not contact the client
// bytesRead = -1,0 -> Always stop loop
// bytesRead cannot already be equal to 0,-1 as the iteration would not continue

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private Proxy proxy;

    public ClientHandler(Socket socket, Proxy proxyServer) {
        clientSocket = socket;
        proxy = proxyServer;
    }

    public void run() {
        try (InputStream clientInputStream = this.clientSocket.getInputStream();
            OutputStream clientOutputStream = this.clientSocket.getOutputStream()) {
            boolean keepAlive = true;
            while (keepAlive) {
                try {
                    clientSocket.setSoTimeout(proxy.getTimeOut());
                    byte[] buffer = new byte[1024];
                    int bytesRead = clientInputStream.read(buffer);
                    
                    if (bytesRead == -1 || bytesRead == 0) break;
                    
                    String requestString = new String(buffer, 0, bytesRead);
                    Request request = new Request(requestString);

                    if (request.isEmpty() || request.isInvalid()) break;

                    if (request.contentExpected()) {
                        while (!request.messageComplete())  {
                            System.out.println("Need more message dude");
                            bytesRead = clientInputStream.read(buffer);
                            if (bytesRead == -1 || bytesRead == 0)
                                break;
                            requestString = new String(buffer, 0, bytesRead);
                            request.addToMessage(requestString);
                        }
                    }

                    if (request.getRequestType().equals("CONNECT")) {
                        handleConnect(clientInputStream, clientOutputStream, request);

                    }

                    Response response;
                    Cache cache = proxy.getCache();
                    String cachedlog = "-";
                    if (request.getRequestType().equals("GET")) {
                        cache.lock();
                        if (cache.responseInCache(request)) {
                            cachedlog = "H";
                            response = cache.getResponse(request);
                        } else {
                            cachedlog = "M";
                            try (Socket originServerSocket = new Socket(request.getDestinationName(), request.getDestinationPort())) {
                                response = handleOrigin(originServerSocket, request, proxy);
                            }
                        }
                        cache.unlock();
                    } else {
                        try (Socket originServerSocket = new Socket(request.getDestinationName(), request.getDestinationPort())) {
                            response = handleOrigin(originServerSocket, request, proxy);
                        }    
                    }
                    clientOutputStream.write(response.buildClientResponse().getBytes());
                    clientOutputStream.flush();

                    if (request.connectionClose()) {
                        keepAlive = false;
                        System.out.println("Client requested connection close.");
                    }
                } catch (SocketTimeoutException e) {
                    // System.out.println("Keep-alive timeout (" + (proxy.getTimeOut()/1000) + "s) reached - closing connection.");
                    break;
                } catch (IOException e) {
                    System.out.println("Connection error: " + e.getMessage());
                    break;
                }
                // } finally {
                //     clientInputStream.close();
                //     clientOutputStream.close();
                // }
            }
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }  finally {
            try {
                if (!clientSocket.isClosed()) this.clientSocket.close();
                
            } catch (IOException e) {
                System.err.println("Exception on client socket close: " + e.getMessage());
            }
        
        }   
    }

    private static Response handleOrigin(Socket originServerSocket, Request request, Proxy proxy) {
        try (InputStream originInputStream = originServerSocket.getInputStream();
            OutputStream originOutputStream = originServerSocket.getOutputStream()
        ) {
            originOutputStream.write(request.buildServerRequest().getBytes());
            originOutputStream.flush();

            byte[] buffer = new byte[1024];
            int bytesRead = originInputStream.read(buffer);

            if (bytesRead == -1 || bytesRead == 0) return null;
            
            String responseString = new String(buffer, 0, bytesRead);
            Response response = new Response(responseString, request);
            
            if (response.isInvalid()) return null;

            if (response.contentExpected()) {
                while (!response.messageComplete())  {
                    System.out.println("Need more mannnn");
                    bytesRead = originInputStream.read(buffer);
                    if (bytesRead == -1 || bytesRead == 0)
                        break;
                    responseString = new String(buffer, 0, bytesRead);
                    response.addToMessage(responseString);
                }
            }
            
            return response;
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            return new Response();
        }
    }

    private void handleConnect(InputStream clientInputStream, OutputStream clientOutputStream, Request request) {
        // Construct response to send to client
        // Check if request is sent to the server this.proxy port and stuff not equal to request port
        if (request.getDestinationPort() != 8443) {
            // Wrong port, send error file
            System.err.println("Incorrect port for connections");
            return;
        }
        // Create a response to send to client
        try (Socket originServerSocket = new Socket(request.getDestinationName(), request.getDestinationPort());
            InputStream serverInputStream = originServerSocket.getInputStream();
            OutputStream serverOutputStream = originServerSocket.getOutputStream()) {
            
            Thread clientToServerThread = new Thread(new ConnectionThread(clientOutputStream, serverInputStream));
            Thread serverToClientThread = new Thread(new ConnectionThread(serverOutputStream, clientInputStream));
            clientToServerThread.start();
            serverToClientThread.start();
            
            clientToServerThread.join();
            serverToClientThread.join();
        } catch (ConnectException e) {
            // 504 Error server connection refused

        } catch (IOException e) {
            // IO Error with streams

        } catch (InterruptedException e) {
            // do something
        }
    }

    private class ConnectionThread implements Runnable {
        private OutputStream outputStream;
        private InputStream inputStream;

        public ConnectionThread(OutputStream outputStreamArg, InputStream inputStreamArg) {
            outputStream = outputStreamArg;
            inputStream = inputStreamArg;
        }

        public void run() {
            while (true) {
                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    if (bytesRead == -1 || bytesRead == 0) break;

                    String intermediateString = new String(buffer, 0, bytesRead);
                    outputStream.write(intermediateString.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    return;
                }
            }
        }
    }
}  