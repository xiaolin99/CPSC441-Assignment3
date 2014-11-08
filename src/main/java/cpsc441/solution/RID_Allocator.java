/**
 * 
 */
package cpsc441.solution;

/**
 * @author Xiao
 *
 */
public class RID_Allocator {
	public final static int MAX_ROUTERS = 10;
	private static boolean router_ids[] = new boolean[MAX_ROUTERS];
	
	public static boolean alloc_rid(int rid) {
		if (rid >= MAX_ROUTERS || rid < 0) return false;
		if (router_ids[rid]) return false;
		else {
			router_ids[rid] = true;
			return true;
		}
	}
	
	public static boolean dealloc_rid(int rid) {
		if (rid >= MAX_ROUTERS || rid < 0) return false;
		if (!router_ids[rid]) return false;
		else {
			router_ids[rid] = false;
			return true;
		}
	}

}
