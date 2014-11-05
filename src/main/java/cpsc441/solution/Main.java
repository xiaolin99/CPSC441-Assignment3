package main.java.cpsc441.solution;

import main.java.cpsc441.doNOTmodify.*;


import java.net.DatagramSocket;

public class Main {
	
    public static void main(String [] args) {
        DatagramSocket s = null;
        // allocating router id

        if(args.length != 4) {
        	System.out.println("Invalid arguments");
        }
        int rid = Integer.parseInt(args[1]);
        if (!RID_Allocator.alloc_rid(rid)) {
        	System.out.println("Router ID must be between 0 and MAX_ROUTER-1 and unique");
        }
       

        //You shouldn't need to modify this code.
        IUDPSocket sock = new UDPSocket(s);
        ILogFactory logFactory = new LogFactory();
        Router r = new Router(sock, logFactory);
        r.run();
    }


}
