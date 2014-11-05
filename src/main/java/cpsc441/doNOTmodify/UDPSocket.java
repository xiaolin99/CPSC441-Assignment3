package main.java.cpsc441.doNOTmodify;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPSocket implements IUDPSocket {

    private final static int MAX_IP4_DATAGRAM_SIZE = 65507;
    private final DatagramSocket sock;


    public UDPSocket(DatagramSocket sock) {
        this.sock = sock;

    }

    public void setSoTimeout(int msTimeout) throws SocketException {
        sock.setSoTimeout(msTimeout);
    }

    public void send(DVRInfo info) throws IOException {
        byte [] data = info.getBytes();
        DatagramPacket pkt = new DatagramPacket(data, data.length);
        sock.send(pkt);
    }

    public DVRInfo receive() throws IOException {
        byte[] buf = new byte[MAX_IP4_DATAGRAM_SIZE];
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        sock.receive(pkt);
        return new DVRInfo(pkt.getData());
    }
}
