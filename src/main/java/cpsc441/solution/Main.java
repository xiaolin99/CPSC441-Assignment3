package main.java.cpsc441.solution;

import main.java.cpsc441.doNOTmodify.ILogFactory;
import main.java.cpsc441.doNOTmodify.IUDPSocket;
import main.java.cpsc441.doNOTmodify.LogFactory;
import main.java.cpsc441.doNOTmodify.UDPSocket;

import java.net.DatagramSocket;

public class Main {

    public static void main(String [] args) {
        DatagramSocket s = null;

        //You shouldn't need to modify this code.
        IUDPSocket sock = new UDPSocket(s);
        ILogFactory logFactory = new LogFactory();
        Router r = new Router(sock, logFactory);
        r.run();
    }


}
