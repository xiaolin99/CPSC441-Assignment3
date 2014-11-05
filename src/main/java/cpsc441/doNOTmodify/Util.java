package main.java.cpsc441.doNOTmodify;

public class Util {
	private Util() {
	}

	public static String printdv(int routerid, int[] mincost, int[] nexthop) {
		return String.format("[%s] table [%s] via [%s]", routerid, HelperUtils.join(mincost, ","),
				HelperUtils.join(nexthop, ","));
	}

}
