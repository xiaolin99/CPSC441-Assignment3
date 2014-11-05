package main.java.cpsc441.doNOTmodify;
import java.util.Arrays;

/**
 * @author: Majid Ghaderi
 * 
 * This class represents the messages passed in between the routers/nem to run
 * the distributed DV algorithm. The instances of the class are immutable. That
 * is, you can modify the content of the object after instantiation using the
 * <code>setByte()</code> method or by modifying the public fields. The byte
 * array returned by the <code>getBytes()</code> methods can be sent using a
 * <code>DatagramPacket</code> over the network and be converted back to an
 * instance of <code>DVRInfo</code> by using the
 * <code>DVRInfo(byte[] buf)</code> constructor.
 *
 */
public class DVRInfo {
	// max number of routers in the network
    public static final int MAX_ROUTERS = 10;

    // infinity link cost
    public static final int COST_INFTY = 999;
    
	// type of packets sent and received
	public static final int PKT_HELLO = 1;
	public static final int PKT_QUIT = 2;
	public static final int PKT_ROUTE = 3;

    // routing information
	public int 	 sourceid;							// ID of the router sending this packet
	public int 	 destid;							// ID of the router to which the packet is being sent
	public int 	 seqnum;							// sequence number of the packet being sent
	public int 	 type;								// type of the packet
	public int[] mincost = new int[MAX_ROUTERS];	// min cost to other routers

	public DVRInfo() {
		initMinCost();
	}

	public DVRInfo(DVRInfo dvr) {
		this.sourceid = dvr.sourceid;
		this.destid = dvr.destid;
		this.seqnum = dvr.seqnum;
		this.type = dvr.type;
		System.arraycopy(dvr.mincost, 0, mincost, 0, mincost.length);
	}

	public DVRInfo(int sourceid, int destid, int seqnum, int type) {
		this.sourceid = sourceid;
		this.destid = destid;
		this.seqnum = seqnum;
		this.type = type;
		initMinCost();
	}

	public DVRInfo(byte[] buf) {
		setBytes(buf);
	}

	public DVRInfo(byte[] buf, int from, int len) {
		setBytes(buf, from, len);
	}

	public byte[] getBytes() {
		Object typeStr = getTypeStr();
		String mincostStr = HelperUtils.join(mincost, " ");
		String packet = String.format("%s%n%d%n%d%n%d%n%s", typeStr, sourceid, destid, seqnum, mincostStr);
		return packet.getBytes();
	}

	public void setBytes(byte[] buf) {
		setBytes(buf, 0, buf.length);
	}

	public void setBytes(byte[] buf, int from, int len) {
		String msg = new String(buf, from, len);
		String[] lines = msg.split("\\r?\\n");

		this.type = getType(lines[0]); // first line
		this.sourceid = HelperUtils.easyToInt(lines[1], "sourceid"); // second line
		this.destid = HelperUtils.easyToInt(lines[2], "destid"); // 3rd line
		this.seqnum = HelperUtils.easyToInt(lines[3], "seqnum"); // 4th line

		if (type == PKT_ROUTE) {
			// then it has a 5th line containing costs
			String[] weights = lines[4].trim().split("\\s+");
			mincost = new int[weights.length];
			for (int i = 0; i < weights.length; i++) {
				mincost[i] = HelperUtils.easyToInt(weights[i], "weights[" + i + "]");
			}
		}
	}

	public String toString() {
		return String.format("%s[%d](%d->%d) %s", getTypeStr(), seqnum, sourceid, destid, Arrays.toString(mincost));
	}

	private int getType(String str) {
		if ("hello".equals(str)) {
			return PKT_HELLO;
		} else if ("quit".equals(str)) {
			return PKT_QUIT;
		} else if ("route".equals(str)) {
			return PKT_ROUTE;
		} else {
			throw new RuntimeException("Unknown type " + str);
		}
	}

	private String getTypeStr() {
		return type == PKT_HELLO ? "hello" : (type == PKT_QUIT ? "quit" : "route");
	}

	private void initMinCost() {
		for (int i = 0; i < MAX_ROUTERS; i++) {
			mincost[i] = COST_INFTY;
		}
	}
}
