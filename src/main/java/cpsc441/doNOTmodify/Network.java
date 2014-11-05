package main.java.cpsc441.doNOTmodify;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * NEM is the starting point of the network emulator. It opens a UDP port and
 * routes the packets among the routers. The port# must be passed through the
 * command line parameter to the class.
 */
public class Network {
	public static void main(String[] args) throws NumberFormatException, IOException, InterruptedException {

		if (args.length != 1) {
			System.out.println("Usage nem <port_number>");
			System.exit(1);
		}

		int portNumber = HelperUtils.easyToInt(args[0], "port number");
		Topology topology = new Topology(new File("topology.txt"));

		// Handing the port is in here
		PortHandler portHandler = new PortHandler(portNumber, topology);
		portHandler.start();

		// quit if enter is hit
		System.out.println("Press enter to quit ...");
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		System.exit(portHandler.quit());
	}
}
