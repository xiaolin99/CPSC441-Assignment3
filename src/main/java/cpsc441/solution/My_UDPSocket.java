package cpsc441.solution;

import java.io.IOException;
import java.net.*;
import cpsc441.doNOTmodify.*

public class My_UDPSocket{
	

    private final static int MAX_IP4_DATAGRAM_SIZE = 65507;
    private final DatagramSocket sock;
    private InetAddress dest_IP;
    private int dest_port;


    public My_UDPSocket(DatagramSocket sock) {
        this.sock = sock;
        dest_IP = InetAddress.getLocalHost();
        dest_port = 5555;
    }
    
    public void setDest(InetAddress IP, int port) {
    	dest_IP = IP;
    	dest_port = port;
    }

    public void setSoTimeout(int msTimeout) throws SocketException {
        sock.setSoTimeout(msTimeout);
    }

    public void send(DVRInfo info) throws IOException {
        byte [] data = info.getBytes();
        DatagramPacket pkt = new DatagramPacket(data, data.length);
        pkt.setAddress(dest_IP);
        pkt.setPort(dest_port);
        sock.send(pkt);
    }

    public DVRInfo receive() throws IOException {
        byte[] buf = new byte[MAX_IP4_DATAGRAM_SIZE];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        sock.receive(pkt);
        return new DVRInfo(pkt.getData());
    }
}
