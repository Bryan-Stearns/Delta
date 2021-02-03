package edu.umich.eecs.soar.delta;

import java.util.ArrayList;
import java.util.List;

public class ContextMemory {
	
	private List<ElaborationContext> elabContexts;
	private List<Prim> prims;
	private List<WMEAddress> addresses;
	
	public ContextMemory() {
		elabContexts = new ArrayList<ElaborationContext>();
		prims = new ArrayList<Prim>();
		addresses = new ArrayList<WMEAddress>();
		//addresses.add(new WMEAddress("state"));
		//addresses.add(new WMEAddress("const"));
	}
	
	public void clear() {
		elabContexts.clear();
		prims.clear();
		addresses.clear();
	}
	
	public void addElabContext(ElaborationContext elab) { elabContexts.add(elab); }
	public void addPrim(Prim prim) { prims.add(prim); }
	
	/**
	 * Add the given WMEAddress to the list.
	 * If the address is already in the list, it will not be added.
	 * @param address The WMEAddress to add
	 */
	public void addAddress(WMEAddress address) {
		int index = addresses.indexOf(address);
		if (index != -1) {
			return;
		}
		addresses.add(address);
	}

	public List<String> getSmemStrings() {
		List<String> retval = new ArrayList<String>();

		retval.add("##################### PROBLEM-SPACES #####################\r\n\r\n");
		for (ElaborationContext elab : elabContexts) {
			// Add a subheader
			retval.add("###\r\n# " + elab.getName().toUpperCase() + "\r\n###\r\n");
			retval.add(elab.toSmemString());
		}
		
		// Print the Prims for all contexts
		retval.add("###################### CONDITION PRIMS ####################\r\n\r\n");
		for (Prim prim : prims) {
			retval.add(prim.toSmemString());
		}
		
		return retval;
	}
}
