package main.java.cpsc441.doNOTmodify;

import static java.lang.String.format;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Properties;

/**
 * This class some useful utility methods which are used by other classes.
 */
class HelperUtils {
	/**
	 * Prevent instantiation by making the constructor private.
	 */
	private HelperUtils() {
	}

	/**
	 * Used to convert a string to an int. Will produce a human understandable
	 * name upon failure.
	 *
	 * @param num
	 * @param name
	 *            : the name of the variable containing the number
	 * @return
	 */
	public static int easyToInt(String num, String name) {
		try {
			return Integer.valueOf(num);
		} catch (NumberFormatException e) {
			throw new RuntimeException(String.format("Cannot parse %s: %s", name, num));
		}
	}

	public static <T> String join(T[] arr, String sep) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			sb.append(arr[i]);
			if (i + 1 != arr.length) {
				sb.append(sep);
			}
		}

		return sb.toString();
	}

	public static DatagramPacket createDatagramPacket(byte buf[]) {
		return new DatagramPacket(buf, buf.length);
	}

	public static DatagramPacket createDatagramPacket(int bufSize) {
		return createDatagramPacket(new byte[bufSize]);
	}

	public static DatagramPacket createDatagramPacket() {
		return createDatagramPacket(2 * 1024);
	}

	public static String join(int[] arr, String sep) {
		if(arr == null)
			return "[<null-array>]";

		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++) {
			sb.append(arr[i]);
			if (i + 1 != arr.length) {
				sb.append(sep);
			}
		}

		return sb.toString();
	}

	public static String toTable(int arr[][]) {
		StringBuilder sb = new StringBuilder();

		for (int[] row : arr) {
			sb.append(join(row, "\t\t"));
			sb.append("\n");
		}

		return sb.toString();
	}

	private static String rep(String str, int times) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < times; i++) {
			sb.append(str);
		}

		return sb.toString();
	}

	/**
	 * This is the old printtd method which prints a 2d array in a neat format.
	 * The new one which works with a 1d array is now placed in
	 * <code>cpsc441.doNOTmodify.DVRInfo</code>
	 *
	 * @param routerid
	 * @param DT
	 * @return
	 */
	public static String printdt(int routerid, int[][] DT) {
		StringBuilder sb = new StringBuilder();
		String hr = "+" + rep("-----+", DT[0].length + 1) + "\n";

		sb.append("Router " + routerid + ":\n");
		sb.append(hr);
		sb.append(format("|%5s|", ""));
		for (int i = 0; i < DT.length; i++) {
			sb.append(format("%-5s|", "R" + i));
		}

		sb.append("\n" + hr);

		for (int i = 0; i < DT.length; i++) {
			sb.append(format("|%-5s|", "R" + i));

			for (int j = 0; j < DT[i].length; j++) {
				sb.append(format("%-5s|", DT[i][j]));
			}
			sb.append("\n");
		}
		sb.append(hr);

		return sb.toString();
	}

	public static int[][] deepClone2DArray(int[][] arr) {
		int[][] copy = (int[][]) arr.clone();
		for (int i = 0; i < arr.length; i++) {
			copy[i] = (int[]) arr[i].clone();
		}
		return copy;
	}

	// read config file
	private static int NEM_ID = 1000;
	private static int ARQ_TIMER = 300;
	private static int COST_INFTY = 999;

	static {
		try {
			Properties properties = new Properties();
			properties.load(new FileReader("config.txt"));
			NEM_ID = Integer.parseInt(properties.getProperty("NEM_ID", "1000"));
			ARQ_TIMER = Integer.parseInt(properties.getProperty("ARQ_TIMER", "300"));
			COST_INFTY = Integer.parseInt(properties.getProperty("COST_INFTY", "999"));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static int getNemId() {
		return NEM_ID;
	}

	public static int getArqTimer() {
		return ARQ_TIMER;
	}

	public static int getCostInfty() {
		return COST_INFTY;
	}
	
	// for test purpose only
	public static void main(String[] args) throws IOException {
		Topology topology = new Topology(new File("topology.txt"));
		System.out.println(printdt(1, topology.getWeightTable()));
		System.out.println("NEM_ID = " + HelperUtils.getNemId());
		System.out.println("ARQ_TIMER = " + HelperUtils.getArqTimer());
	}

}
