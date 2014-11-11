package cpsc441.solution;

/*
 * CPSC441 - Assignment3 Router.java
 * by Xiao Lin
 */

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
	private int[] seqnum;
	private int numNeighbors;
	private int[] neighbors;
	private boolean[] isNeighbor;
	private int[][] DV;
	private int[] nexthops;
	private ILogFactory logFactory;
	private int MAX_ROUTERS;
	
	private final int SEQNUM_MAX = 1000;
	
    public Router(int rid, My_UDPSocket sock, ILogFactory logFactory) {
    	// this code to read config.txt is copied directly from HelperUtils
    	try {
			Properties properties = new Properties();
			properties.load(new FileReader("config.txt"));
			NEM_ID = Integer.parseInt(properties.getProperty("NEM_ID", "1000"));
			ARQ_TIMER = Integer.parseInt(properties.getProperty("ARQ_TIMER", "300"));
			COST_INFTY = Integer.parseInt(properties.getProperty("COST_INFTY", "999"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
    	this.MAX_ROUTERS = DVRInfo.MAX_ROUTERS;    	
    	this.sock = sock;
    	// set timeout here
    	try {
			sock.setSoTimeout(ARQ_TIMER);
		} catch (SocketException e) {
			System.out.println("Unable to set socket timeout");
		}
    	this.rid = rid;
    	this.logFactory = logFactory;
    }
    
    /**
     * Functions to init various arrays of this router
     */
    private void initDV() {
    	for (int i = 0; i < MAX_ROUTERS; i ++) {
    		for (int j = 0; j < MAX_ROUTERS; j ++) DV[i][j] = COST_INFTY;
    	}
    }
    private void initNeighbor() {
    	numNeighbors = 0;
    	for (int i = 0; i < MAX_ROUTERS; i ++) neighbors[i] = -1;
    	for (int i = 0; i < MAX_ROUTERS; i ++) isNeighbor[i] = false;
    }
    private void initNextHop() {
    	for (int i = 0; i < MAX_ROUTERS; i ++) nexthops[i] = -1;
    }
    private void initSeqnum() {
    	for (int i = 0; i < MAX_ROUTERS; i ++) seqnum[i] = 0;
    	seqnum[rid] = rid+100; // so each router will start with their own unique seqnum
    }
    private void initAll() {
    	seqnum = new int[MAX_ROUTERS];
    	neighbors = new int[MAX_ROUTERS];
    	isNeighbor = new boolean[MAX_ROUTERS];
    	DV = new int[MAX_ROUTERS][MAX_ROUTERS];
    	nexthops = new int[MAX_ROUTERS];
    	initDV();
    	initNeighbor();
    	initNextHop();
    	initSeqnum();
    }
    
    /**
     * increment seqnum, warp when reached SEQNUM_MAX
     */
    private void incrementSeqnum() {
    	seqnum[rid] ++;
    }
    
    /**
     * recalculate DV
     * @return boolean - true if DV has changed
     */
    private boolean recalculateDV()  {
    	boolean changed = false;
    	// rid -> current router, y -> destination, neighbors[v] -> neighbor
    	for (int y = 0; y < MAX_ROUTERS; y ++) {
    		for (int v = 0; v < numNeighbors; v ++){
    			int dvXY = DV[rid][neighbors[v]] + DV[neighbors[v]][y];
    			if (dvXY > COST_INFTY) dvXY = COST_INFTY;
    			if (DV[rid][y] > dvXY){
    				DV[rid][y] = dvXY;
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
    		DVRInfo snd_packet = new DVRInfo(rid, neighbors[v], seqnum[rid], DVRInfo.PKT_ROUTE);
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
    

    /**
     * Router running ...
     */
    public void run() {
    	// setup log
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
    			// UDP reliablility (timeout)
    			System.out.println("NEM unresponsive!");
    			continue;
    		} catch (IOException e) {
    			System.out.println("failed to send hello packet.");
    			my_sleep(ARQ_TIMER);
    			continue;
    		} 
    	}
    	
    	// setup neighbor info and initialize all arrays
    	MAX_ROUTERS = rcv_packet.mincost.length;
    	initAll();
    	DV[rid] = rcv_packet.mincost;
		for (int i = 0; i < rcv_packet.mincost.length; i ++) {
			if (rcv_packet.mincost[i] < COST_INFTY) {
				neighbors[numNeighbors] = i;
				numNeighbors ++;
				isNeighbor[i] = true;
				nexthops[i] = i;
			}
		}
		System.out.println("Neighbors: "+numNeighbors);
		
		// send DV to neighbors
		this.sendDVToNeighbors(log);
		// starting receive packet from neighbors
		while (true) {
			try {
				rcv_packet = sock.receive();
				// check if packet is intended for this router
				if (rcv_packet.destid == rid) {
					// quit if received PKT_QUIT
					if (rcv_packet.type == DVRInfo.PKT_QUIT) break;
					if (rcv_packet.sourceid == NEM_ID) continue;
					if (isNeighbor[rcv_packet.sourceid] && rcv_packet.type == DVRInfo.PKT_ROUTE) {
						// BONUS: check seqnum, if unchanged, skip loop
						if (rcv_packet.seqnum == seqnum[rcv_packet.sourceid]) continue;
						else seqnum[rcv_packet.sourceid] = rcv_packet.seqnum;
						System.out.println("Received New DV!");
						if (log != null) log.printf("[%d] receive %s]\n", rid, rcv_packet);
						// calculate DV and send to neighbors if DV changed
						DV[rcv_packet.sourceid] = rcv_packet.mincost;
						if (this.recalculateDV()) {
							// increment seqnum if DV changed, then sent to neighbours
							System.out.println("DV changed!");
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
