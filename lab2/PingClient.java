import java.net.*;
import java.util.*;

public class PingClient {

	public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Intended usage: java PingClient <host> <port>");
        }
        InetAddress IPAddress = InetAddress.getByName(args[0]);
		int serverPort = Integer.parseInt(args[1]); 

		DatagramSocket clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(600);

        Random rand  = new Random();
        int sequenceBase = 40000 + rand.nextInt(10000);
        int pingCount = 15;
        
        List<Long> rttList = new ArrayList<Long>();
        int packetsLost = 0;
        
        long startTransmissionTime = System.currentTimeMillis();

        for (int i = 0; i < pingCount; i++) {
            int seq = sequenceBase + i;
            long timestamp = System.currentTimeMillis();
            String message = "PING " + seq + " " + timestamp;

            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                long sendTime = System.currentTimeMillis();
                clientSocket.send(sendPacket);

                clientSocket.receive(receivePacket);
                long receiveTime = System.currentTimeMillis();

                long rtt = receiveTime - sendTime;
                rttList.add(rtt);
                
                System.out.println("PING to " + args[0] + ", seq=" + seq + ", rtt=" + rtt + " ms");
            } catch (SocketTimeoutException e) {
                System.out.println("PING to " + args[0] + ", seq=" + seq + ", rtt=timeout");
                packetsLost++;
            }

        }

        long endTransmissionTime = System.currentTimeMillis();
        clientSocket.close();

        long minRTT = 0;
        long maxRTT = 0;

        if (rttList.size() > 0) {
            Collections.sort(rttList);

            minRTT = rttList.get(0);
            maxRTT = rttList.get(rttList.size() - 1);
        }
        double avgRTT = rttList.stream().mapToLong(Long::longValue).average().orElse(0.0);

        double jitter = 0.0;
        if (rttList.size() > 1) {
            int diff = 0;
            for (int i = 1; i < rttList.size(); i++) {
                diff += Math.abs(rttList.get(i) - rttList.get(i - 1));
            }
            jitter = diff / (rttList.size() - 1);
        }

        double lossPercent = ((double) packetsLost / pingCount) * 100;
        long totalTime = endTransmissionTime - startTransmissionTime;
        System.out.println("...");
        System.out.println("Packet loss: " + (int) lossPercent + "%");
        System.out.println("Minimum RTT: " + minRTT + " ms, Maximum RTT: " + maxRTT + " ms, Average RTT: " + (int) avgRTT + " ms");
        System.out.println("Total transmission time: " + totalTime + " ms");
        System.out.println("Jitter: " + (int) jitter + " ms");	
	}
}