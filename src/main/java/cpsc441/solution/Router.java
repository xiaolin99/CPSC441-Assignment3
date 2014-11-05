package main.java.cpsc441.solution;


import main.java.cpsc441.doNOTmodify.*;

public class Router implements Runnable {
	
	private static int NEM_ID;
	private static int ARQ_TIMER;
	private static int COST_INFTY;
	
    public Router(IUDPSocket sock, ILogFactory logFactory) {
    	NEM_ID = HelperUtils.getNemId();
    	ARQ_TIMER = HelperUtils.getArqTimer();
    	COST_INFTY = HelperUtils.getCostInfty();
    }

    public void run() {
    	
    }
}
