import java.io.*;
import java.net.*;
import java.nio.file.Files;

public class WebServer {

	public static void main(String[] args)throws Exception {
        if (args.length != 1) {
            System.out.println("Intended usage: java WebServer <port>");
		    return;
        }
		int serverPort = Integer.parseInt(args[0]); 

		ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            while (true) {
                String requestLine = inFromClient.readLine();
                if (requestLine == null || requestLine.isEmpty()) {
                    break;
                }
                

                String headerLine;
                boolean connectionClose = false;
                while ((headerLine = inFromClient.readLine()) != null && !headerLine.isEmpty()) {
                    connectionClose = headerLine.toLowerCase().startsWith("connection: close");
                }

                String filename = requestLine.split(" ")[1].substring(1);
                System.out.println(filename);
                File file = new File(filename);
                
                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
                    outToClient.writeBytes("Content-Type: " + getContentType(filename) + "\r\n");
                    outToClient.writeBytes("Content-Length: " + fileContent.length + "\r\n");
                    outToClient.writeBytes("Connection: " + (connectionClose ? "close" : "keep-alive") + "\r\n\r\n");
                    outToClient.write(fileContent);
                } else {
                    String body = "<h1>404 Not Found</h1>";
                    byte[] bodyBytes = body.getBytes();
                    outToClient.writeBytes("HTTP/1.1 404 Not Found\r\n");
                    outToClient.writeBytes("Content-Type: text/html\r\n");
                    outToClient.writeBytes("Content-Length: " + bodyBytes.length + "\r\n");
                    outToClient.writeBytes("Connection: " + (connectionClose ? "close" : "keep-alive") + "\r\n\r\n");
                    outToClient.write(bodyBytes);
                }
                outToClient.flush();

                if (connectionClose)
                    break;
            }
            connectionSocket.close();
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
