package main.java.cpsc441.solution;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import main.java.cpsc441.doNOTmodify.*;

public class Router implements Runnable {
	
	private int NEM_ID;
	private int ARQ_TIMER;
	private int COST_INFTY;
	private IUDPSocket sock;
	private int rid;
	private int seqnum;
	private int numNeighbors;
	private int[] neighbors = new int[RID_Allocator.MAX_ROUTERS];
	private boolean[] isNeighbor = new boolean[RID_Allocator.MAX_ROUTERS];
	private int DV[][] = new int[RID_Allocator.MAX_ROUTERS][RID_Allocator.MAX_ROUTERS];
	private int nexthops[] = new int[RID_Allocator.MAX_ROUTERS];
	
    public Router(int rid, IUDPSocket sock, ILogFactory logFactory) {
    	NEM_ID = HelperUtils.getNemId();
    	ARQ_TIMER = HelperUtils.getArqTimer();
    	COST_INFTY = HelperUtils.getCostInfty();
    	this.sock = sock;
    	try {
			sock.setSoTimeout(ARQ_TIMER);
		} catch (SocketException e) {
			System.out.println("Unable to set socket timeout");
		}
    	this.rid = rid;
    	seqnum = 0;
    	initDV();
    	initNeighbor();
    	initNextHop();
    }
    
    /**
     * Init various arrays of this router
     */
    private void initDV() {
    	for (int i = 0; i < RID_Allocator.MAX_ROUTERS; i ++) {
    		for (int j = 0; j < RID_Allocator.MAX_ROUTERS; j ++) DV[i][j] = COST_INFTY;
    	}
    }
    private void initNeighbor() {
    	numNeighbors = 0;
    	for (int i = 0; i < RID_Allocator.MAX_ROUTERS; i ++) neighbors[i] = -1;
    	for (int i = 0; i < RID_Allocator.MAX_ROUTERS; i ++) isNeighbor[i] = false;
    }
    private void initNextHop() {
    	for (int i = 0; i < RID_Allocator.MAX_ROUTERS; i ++) nexthops[i] = -1;
    }
    
    /**
     * recalculate DV
     * @return boolean - true if DV has changed
     */
    private boolean recalculateDV()  {
    	boolean changed = false;
    	// rid -> current router, y -> destination, neighbors[v] -> neighbor
    	for (int y = 0; y < RID_Allocator.MAX_ROUTERS; y ++) {
    		for (int v = 0; v < numNeighbors; v ++){
    			int DV_x_to_y = DV[rid][neighbors[v]]+DV[neighbors[v]][y];
    			if (DV_x_to_y > COST_INFTY) DV_x_to_y = COST_INFTY;
    			if (DV[rid][y] > DV_x_to_y){
    				DV[rid][y] = DV_x_to_y;
    				nexthops[y] = neighbors[v];
    				changed = true;
    			}
    		}
    	}
    	return changed;
    }
    
    /**
     * send DV to neighbors
     */
    private void sendDVToNeighbors() {
    	for (int v = 0; v < numNeighbors; v ++){
    		DVRInfo snd_packet = new DVRInfo(rid, neighbors[v], seqnum, DVRInfo.PKT_ROUTE);
			snd_packet.mincost = DV[rid];
			try {
				sock.send(snd_packet);
			} catch (IOException e) {
				System.out.println("Unable to send DV to neighbor " +neighbors[v]);
			}
    	}
    }
    
    /**
     * Just a wrapper for thread.sleep(), for the purpose of clean code
     * @param ms
     */
    private void my_sleep(long ms){
    	try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// do nothing
		}
    }
    

    public void run() {
    	try {
			PrintStream log = new LogFactory().newLogFile((new File("Router"+rid+".log")));
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
    	
    	// send hello packet and receive neighbor info
    	DVRInfo snd_packet = null;
    	DVRInfo rcv_packet = null;
    	
    	snd_packet = new DVRInfo(rid, NEM_ID, seqnum, DVRInfo.PKT_HELLO);
    	boolean success = false;
    	while (!success) {
    		try {
    			sock.send(snd_packet);
    			rcv_packet = sock.receive();
    			if (rcv_packet.sourceid == NEM_ID 
    					&& rcv_packet.destid == rid 
    					&& rcv_packet.type == DVRInfo.PKT_ROUTE) success = true;
    		} catch (SocketTimeoutException e1) {
    			System.out.println("socket timeout");
    			continue;
    		} catch (IOException e) {
    			System.out.println("failed to send hello packet.");
    			my_sleep(ARQ_TIMER);
    			continue;
    		} 
    	}
    	
		for (int i = 0; i < rcv_packet.mincost.length; i ++) {
			if (rcv_packet.mincost[i] < COST_INFTY) {
				neighbors[numNeighbors] = i;
				numNeighbors ++;
				isNeighbor[i] = true;
				nexthops[i] = i;
				DV[rid][i] = rcv_packet.mincost[i];
			}
		}
		
		// send DV to neighbors
		this.sendDVToNeighbors();
		// starting receive packet from neighbors
		while (true) {
			try {
				rcv_packet = sock.receive();
				// check if packet is intended for this router
				if (rcv_packet.destid == rid) {
					if (isNeighbor[rcv_packet.sourceid] && rcv_packet.type == DVRInfo.PKT_ROUTE) {
						log.printf("a");
						// calculate DV and send to neighbors if DV changed
						DV[rcv_packet.sourceid] = rcv_packet.mincost;
						if (this.recalculateDV()) {
							this.sendDVToNeighbors();
						}
					}
					if (rcv_packet.type == DVRInfo.PKT_QUIT) break;
				}
			} catch (SocketTimeoutException e1) {
				// on timeout, resend to neighbors
				this.sendDVToNeighbors();
				continue;
			} catch (IOException e) {
				continue;
			}
			
		}
		Util.printdv(rid, DV[rid], nexthops);
    }
}
