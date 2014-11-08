package main.java.cpsc441.solution;

import java.io.*;
import java.net.*;
import main.java.cpsc441.doNOTmodify.*;

public class Router implements Runnable {
	
	private int NEM_ID;
	private int ARQ_TIMER;
	private int COST_INFTY;
	private IUDPSocket sock;
	private int rid;
	private int[] seqnum = new int[RID_Allocator.MAX_ROUTERS];
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
    	initDV();
    	initNeighbor();
    	initNextHop();
    	initSeqnum();
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
    private void initSeqnum() {
    	for (int i = 0; i < RID_Allocator.MAX_ROUTERS; i ++) seqnum[i] = 0;
    }
    
    /**
     * Function to increment seqnum
     */
    private void incrementSeqnum() {
    	
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
     * send DV to neighbours
     */
    private void sendDVToNeighbors(PrintWriter log) {
    	for (int v = 0; v < numNeighbors; v ++){
    		DVRInfo snd_packet = new DVRInfo(rid, neighbors[v], seqnum[neighbors[v]], DVRInfo.PKT_ROUTE);
			snd_packet.mincost = DV[rid];
			try {
				if (log != null) log.printf("[%d] send %s]\n", rid, snd_packet);
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
    	PrintStream logStream = null;
    	PrintWriter log = null;
    	try {
			logStream = new LogFactory().newLogFile((new File("Router"+rid+".log")));
			log = new PrintWriter(logStream);
		} catch (FileNotFoundException e2) {
			System.out.println("Unable to create log file");
		} catch (IOException e2) {
			e2.printStackTrace();
		}
    	
    	// send hello packet and receive neighbor info
    	DVRInfo snd_packet = null;
    	DVRInfo rcv_packet = null;
    	
    	snd_packet = new DVRInfo(rid, NEM_ID, 0, DVRInfo.PKT_HELLO);
    	boolean success = false;
    	while (!success) {
    		try {
    			if (log != null) log.printf("[%d] send %s]\n", rid, snd_packet);
    			sock.send(snd_packet);
    			rcv_packet = sock.receive();
    			if (log != null) log.printf("[%d] receive %s]\n", rid, rcv_packet);
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
		this.sendDVToNeighbors(log);
		// starting receive packet from neighbors
		while (true) {
			try {
				rcv_packet = sock.receive();
				// check if packet is intended for this router
				if (rcv_packet.destid == rid) {
					if (log != null) log.printf("[%d] receive %s]\n", rid, rcv_packet);
					if (isNeighbor[rcv_packet.sourceid] && rcv_packet.type == DVRInfo.PKT_ROUTE) {
						// calculate DV and send to neighbors if DV changed
						DV[rcv_packet.sourceid] = rcv_packet.mincost;
						if (this.recalculateDV()) {
							// increment seq if DV changed
							
							this.sendDVToNeighbors(log);
						}
					}
					if (rcv_packet.type == DVRInfo.PKT_QUIT) break;
				}
			} catch (SocketTimeoutException e1) {
				// on timeout, resend to neighbors
				this.sendDVToNeighbors(log);
				continue;
			} catch (IOException e) {
				continue;
			}
			
		}
		log.flush();
		log.close();
		logStream.flush();
		logStream.close();
		Util.printdv(rid, DV[rid], nexthops);
    }
}
