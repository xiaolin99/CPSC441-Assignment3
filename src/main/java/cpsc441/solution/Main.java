package cpsc441.solution;

/*
 * CPSC441 - Assignment3 main
 * by Xiao Lin
 */

import cpsc441.doNOTmodify.*;

import java.net.*;


public class Main {
	
    public static void main(String [] args) {
        DatagramSocket s = null;
        InetAddress IP = null;
        int port = 0;
        // allocating router id

        if(args.length != 3) {
        	System.out.println("Invalid arguments");
        	System.exit(1);
        }
        int rid = Integer.parseInt(args[0]);
        if (rid < 0 || rid >= DVRInfo.MAX_ROUTERS) {
        	System.out.println("Router ID must be between 0 and MAX_ROUTER-1");
        	System.exit(1);
        }
        try {
			IP = InetAddress.getByName(args[1]);
		} catch (UnknownHostException e) {
			System.out.println("Invalid NEM host");
			System.exit(1);
		}
        port = Integer.parseInt(args[2]);
        try {
			s = new DatagramSocket(50000+rid);
		} catch (SocketException e) {
			System.out.println("Unable to create socket - Router ID must be unique!");
			System.exit(1);
		}

        //You shouldn't need to modify this code.
        My_UDPSocket sock = new My_UDPSocket(s);
        sock.setDest(IP, port);
        ILogFactory logFactory = new LogFactory();
        Router r = new Router(rid, sock, logFactory);
        r.run();
    }


}
