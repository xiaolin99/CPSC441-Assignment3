package main.java.cpsc441.solution;


import java.io.IOException;
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
    }
    
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
    
    /**
     * recalculate DV
     * @return boolean - true if DV has changed
     */
    private boolean recalculateDV()  {
    	boolean changed = false;
    	// rid -> current router, y -> destination, neighbors[v] -> neighbor
    	for (int y = 0; y < RID_Allocator.MAX_ROUTERS; y ++) {
    		int minCost = DV[rid][y];
    		for (int v = 0; v < numNeighbors; v ++){
    			minCost = Math.min(minCost, DV[rid][neighbors[v]]+DV[neighbors[j]][v]);
    		}
    		if (minCost != DV[rid][y]) changed = true;
    		DV[rid][y] = minCost;
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

    public void run() {
    	// send hello packet and receive neighbor info
    	DVRInfo snd_packet = null;
    	DVRInfo rcv_packet = null;
    	
    	snd_packet = new DVRInfo(rid, NEM_ID, seqnum, DVRInfo.PKT_HELLO);
    	boolean success = false;
    	while (!success) {
    		try {
    			sock.send(snd_packet);
    			rcv_packet = sock.receive();
    			if (rcv_packet.sourceid == NEM_ID) success = true;
    		} catch (SocketTimeoutException e1) {
    			System.out.println("socket timeout");
    		} catch (IOException e) {
    			System.out.println("failed to send hello packet.");
    		} 
    	}
    	
		for (int i = 0; i < rcv_packet.mincost.length; i ++) {
			if (rcv_packet.mincost[i] < COST_INFTY) {
				neighbors[numNeighbors] = i;
				numNeighbors ++;
				isNeighbor[i] = true;
				DV[rid][i] = rcv_packet.mincost[i];
			}
		}
		
		// send DV to neighbors
		sendDVToNeighbors();
		// starting receive packet from neighbors
		while (true) {
			
			
		}
		
    }
}
