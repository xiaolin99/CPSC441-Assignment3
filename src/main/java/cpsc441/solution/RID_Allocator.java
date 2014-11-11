package cpsc441.solution;

/**
 * CPSC441 - Assignment3
 * This class attemps to check if router id is unique
 * by Xiao Lin
 */

import cpsc441.doNOTmodify.*;

/**
 * @author Xiao
 *
 */
public class RID_Allocator {
	private static boolean router_ids[] = new boolean[DVRInfo.MAX_ROUTERS];
	
	public static boolean alloc_rid(int rid) {
		if (rid >= DVRInfo.MAX_ROUTERS || rid < 0) return false;
		if (router_ids[rid]) return false;
		else {
			router_ids[rid] = true;
			return true;
		}
	}
	
	public static boolean dealloc_rid(int rid) {
		if (rid >= DVRInfo.MAX_ROUTERS || rid < 0) return false;
		if (!router_ids[rid]) return false;
		else {
			router_ids[rid] = false;
			return true;
		}
	}

}
