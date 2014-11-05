package main.java.cpsc441.doNOTmodify;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

/**
 * Most of the logic behind the NEM is placed in this class. This class is
 * responsible for opening the socket, listening to the port, receiving the
 * packets and routing them to other routers. The class enables us to run the
 * logic inside a new thread so we can listen to the console to get the enter
 * key to finish the program.
 */
public class PortHandler extends Thread {
	DatagramSocket socket;
	DatagramPacket packet = HelperUtils.createDatagramPacket();
	DVRInfo info = new DVRInfo();
    //A.Sehati
	boolean receiveAll;
    //A.Sehati
	/**
	 * <code>addressMap</code> and <code>portMap</code> are used to keep the ip
	 * address and port# of the routers. Every time a hello message is received
	 * from a router, the ip address and the port# are put inside these
	 * dictionaries. So, later, when we get a rout message, we know where to
	 * send it.
	 */
	HashMap<Integer, InetAddress> addressMap = new HashMap<Integer, InetAddress>();
	HashMap<Integer, Integer> portMap = new HashMap<Integer, Integer>();
	private final Topology topology;
	private long lastPacketReceivedTimestamp = System.currentTimeMillis();

	public PortHandler(int port, Topology topology) throws SocketException {
		this.topology = topology;
        System.out.println("running on port [ " + port + " ]");
		socket = new DatagramSocket(port);
        //A.Sehati
		receiveAll = false;
        //A.Sehati
	}

	/**
	 * The main loop of the class
	 */
	@Override
	public void run() {


		while (true) {
			try {

                //A.Sehati
                if(this.topology.getHasTwoParts())
                {
                    if ( !receiveAll && (addressMap.size() == topology.getNumRouters()) )
                    {
                        ChangeHandler ch = this.new ChangeHandler();
                        ch.start();
                        receiveAll = true;
                    }
                }
                //A.Sehati

                System.out.println("awaiting packet...");
				socket.receive(packet);

				// update the timestamp so that we know when we received the
				// last packet, used in the quit method
				lastPacketReceivedTimestamp = System.currentTimeMillis();

				info.setBytes(packet.getData(), 0, packet.getLength());

				System.out.println("[NEM] Received: " + info);

                if (info.type == DVRInfo.PKT_HELLO) {
					handleHelloMessage(info, packet);
				} else if (info.type == DVRInfo.PKT_ROUTE) {
					handleRouteMessage(info, packet);
				} else {
					// we shouldn't get a hello quit message at the NEM
					System.out.println("Unknown packet received:\n" + info);
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private void handleRouteMessage(DVRInfo recvInfo, DatagramPacket recvPacket) throws IOException {
		// if we haven't received a hello message from the destination
		if (!addressMap.containsKey(recvInfo.destid))
			throw new RuntimeException(String.format(
					"Router %s tries to send a route message to router %s which is not present yet.",
					recvInfo.sourceid, recvInfo.destid));

		// if the destination is not a neighbor simple drop the packet

        if (topology.getWeight(recvInfo.sourceid, recvInfo.destid) >= HelperUtils.getCostInfty()) {
			System.err.println("Not a neighbor, packet dropped " + recvInfo);
			return;
		}

		// send the packet to dest
		InetAddress destAddr = addressMap.get(recvInfo.destid);
		int destPort = portMap.get(recvInfo.destid);

		DatagramPacket respPacket = recvPacket;
		respPacket.setAddress(destAddr);
		recvPacket.setPort(destPort);
		socket.send(respPacket);
	}

	private void handleHelloMessage(DVRInfo reqInfo, DatagramPacket reqPacket) throws IOException {
		// Put the address/port into the table so that we can use it to find the
		// destination address/port later
		addressMap.put(reqInfo.sourceid, reqPacket.getAddress());
		portMap.put(reqInfo.sourceid, reqPacket.getPort());

		// send the initial costs in response to hello
		DVRInfo respInfo = new DVRInfo();
		respInfo.type = DVRInfo.PKT_ROUTE;
		respInfo.sourceid = HelperUtils.getNemId();
		respInfo.destid = reqInfo.sourceid;
		respInfo.seqnum = 0;
		respInfo.mincost = topology.getWeightsForRouter(reqInfo.sourceid);

		System.out.println("[NEM] sending " + respInfo);
		byte[] bytes = respInfo.getBytes();
		DatagramPacket respPacket = HelperUtils.createDatagramPacket(bytes);
		respPacket.setData(bytes);
		respPacket.setAddress(reqPacket.getAddress());
		respPacket.setPort(reqPacket.getPort());
		socket.send(respPacket);
	}

	/**
	 * In the quit method, we send a quit message to all of the routers. If we
	 * didn't get any packet from the routers for a while, we will proceed
	 * quitting. Otherwise, we will repeat the process for a few (3) times. In
	 * this case, we will have an unsuccessful quit. That is some routers are
	 * still running.
	 *
	 * @return
	 * @throws java.io.IOException
	 * @throws InterruptedException
	 */
	public int quit() throws IOException, InterruptedException {
		int waitTime = HelperUtils.getArqTimer() * 10;
		int retryCount = 0;

		while (true) {
			System.out.printf("[NEM] %ssending quit message to routers.%n", retryCount == 0 ? "" : "re-");
			retryCount++;
			sendQuitMessageToRouters();
			Thread.sleep(waitTime);
			if (lastPacketReceivedTimestamp + waitTime < System.currentTimeMillis()) {
				System.out.println("[NEM] Quit Successful.");
				return 0;
			} else {
				System.out.println("[NEM] Quit Unsuccessful.");
			}

			if (retryCount >= 3) {
				System.out.println("[NEM] Unable to quit. There is a problem with network or router's algorithm.");
				return 1;
			}
		}
	}

	private void sendQuitMessageToRouters() throws IOException {
		for (Integer routerId : portMap.keySet()) {
			DVRInfo info = new DVRInfo(HelperUtils.getNemId(), routerId, 0, DVRInfo.PKT_QUIT);
			DatagramPacket packet = HelperUtils.createDatagramPacket(info.getBytes());
			packet.setAddress(addressMap.get(routerId));
			packet.setPort(portMap.get(routerId));
			socket.send(packet);
		}
	}
	
	//A.Sehati
	private class ChangeHandler extends Thread{
		public void run()
		{
			try {
				Thread.sleep(topology.getChangeTime());
				
				int[] cIndex = topology.getChangedIndex();
				
				for (int i = 0; i < cIndex.length; i++)
				{
					if (cIndex[i] == 1)
					{
						if (!addressMap.containsKey(i))
							throw new RuntimeException(String.format(
									"NEM tries to send a cost change message to router %s which is not present yet.",
									i));
						sendChangedCost(i);
					}						
				}

                topology.changeWeights();

			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			
		}
		
		private void sendChangedCost(int rId) throws IOException
		{
			DVRInfo respInfo = new DVRInfo();
			respInfo.type = DVRInfo.PKT_ROUTE;
			respInfo.sourceid = HelperUtils.getNemId();
			respInfo.destid = rId;
			respInfo.seqnum = 0;
			respInfo.mincost = topology.getChangedWeightsForRouter(rId);
			
			System.out.println("[NEM] sending changed cost: " + respInfo);
			DatagramPacket packet = HelperUtils.createDatagramPacket(respInfo.getBytes());
			packet.setAddress(addressMap.get(rId));
			packet.setPort(portMap.get(rId));
			socket.send(packet);
		}
	}
}
