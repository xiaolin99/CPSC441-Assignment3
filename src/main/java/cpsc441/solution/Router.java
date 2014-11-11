package cpsc441.solution;

import java.io.*;
import java.net.*;
import java.util.Properties;
import cpsc441.doNOTmodify.*;

public class Router implements Runnable {
	
	private int NEM_ID;
	private int ARQ_TIMER;
	private int COST_INFTY;
	private My_UDPSocket sock;
	private int rid;
	private int[] seqnum = new int[RID_Allocator.MAX_ROUTERS];
	private int numNeighbors;
	private int[] neighbors = new int[RID_Allocator.MAX_ROUTERS];
	private boolean[] isNeighbor = new boolean[RID_Allocator.MAX_ROUTERS];
	private int DV[][] = new int[RID_Allocator.MAX_ROUTERS][RID_Allocator.MAX_ROUTERS];
	private int nexthops[] = new int[RID_Allocator.MAX_ROUTERS];
	private ILogFactory logFactory;
	
	private final int SEQNUM_MAX = 1000;
	
    public Router(int rid, My_UDPSocket sock, ILogFactory logFactory) {
    	try {
			Properties properties = new Properties();
			properties.load(new FileReader("config.txt"));
			NEM_ID = Integer.parseInt(properties.getProperty("NEM_ID", "1000"));
			ARQ_TIMER = Integer.parseInt(properties.getProperty("ARQ_TIMER", "300"));
			COST_INFTY = Integer.parseInt(properties.getProperty("COST_INFTY", "999"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
    	
    	this.sock = sock;
    	try {
			sock.setSoTimeout(ARQ_TIMER);
		} catch (SocketException e) {
			System.out.println("Unable to set socket timeout");
		}
    	this.rid = rid;
    	this.logFactory = logFactory;
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
     * increment seqnum, warp when reached SEQNUM_MAX
     */
    private void incrementSeqnum() {
    	for (int i = 0; i < RID_Allocator.MAX_ROUTERS; i ++) {
    		seqnum[i]++;
    		if (seqnum[i] > SEQNUM_MAX) seqnum[i] = 0;
    	}
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
    		String filepath = "./Router" + rid + ".log";
			logStream = logFactory.newLogFile((new File(filepath)));
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
					if (rcv_packet.type == DVRInfo.PKT_QUIT) break;
					if (rcv_packet.sourceid == NEM_ID) continue;
					if (isNeighbor[rcv_packet.sourceid] && rcv_packet.type == DVRInfo.PKT_ROUTE) {
						if (rcv_packet.seqnum == seqnum[rcv_packet.sourceid]) continue;
						else seqnum[rcv_packet.sourceid] = rcv_packet.seqnum;
						// calculate DV and send to neighbors if DV changed
						DV[rcv_packet.sourceid] = rcv_packet.mincost;
						if (this.recalculateDV()) {
							// increment seqnum if DV changed, then sent to neighbours
							incrementSeqnum();
							this.sendDVToNeighbors(log);
						}
					}
					
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
		System.out.println(Util.printdv(rid, DV[rid], nexthops));
    }
}
