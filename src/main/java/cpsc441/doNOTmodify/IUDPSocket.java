package main.java.cpsc441.doNOTmodify;

import java.io.IOException;
import java.net.SocketException;

public interface IUDPSocket {
    public void setSoTimeout(int msTimeout) throws SocketException;
    public DVRInfo receive() throws IOException;
    public void send(DVRInfo info) throws IOException;
}
