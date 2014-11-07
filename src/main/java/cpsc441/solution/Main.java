package main.java.cpsc441.solution;

import main.java.cpsc441.doNOTmodify.*;

import java.net.*;

public class Main {
	
    public static void main(String [] args) {
        DatagramSocket s = null;
        InetAddress IP = null;
        int port = 0;
        // allocating router id

        if(args.length != 3) {
        	System.out.println("Invalid arguments");
        	System.exit(-1);
        }
        int rid = Integer.parseInt(args[1]);
        if (!RID_Allocator.alloc_rid(rid)) {
        	System.out.println("Router ID must be between 0 and MAX_ROUTER-1 and unique");
        }
        try {
			IP = InetAddress.getByName(args[1]);
		} catch (UnknownHostException e) {
			System.out.println("Invalid NEM host");
			System.exit(-1);
		}
        port = Integer.parseInt(args[2]);
        try {
			s = new DatagramSocket(port, IP);
		} catch (SocketException e) {
			System.out.println("Unable to create socket");
			System.exit(-1);
		}

        //You shouldn't need to modify this code.
        IUDPSocket sock = new UDPSocket(s);
        ILogFactory logFactory = new LogFactory();
        Router r = new Router(rid, sock, logFactory);
        r.run();
    }


}
