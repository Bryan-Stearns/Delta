package edu.umich.eecs.soar.delta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WMERef {

	private LispishObject ins_wm;					// The WM tree that represents the WMEAddresses needed for just this (ref)
	private List<WMEAddress> addresses;				// A list of the addresses defined for this (ref)
	private Map<String, WMEAddress> varNameMap;		// A map of <var> names to the WMEAddress that they reference; specific to one (ins)
	
	/**
	 * This object holds the LispishObject tree representation of referenced WM structures within the scope of a single instructed rule.
	 * @param name The variable name for this WME. For example, for "S1.foo.bar <name>", enter "<name>".
	 */
	public WMERef() {
		this.ins_wm = new LispishObject("S1");
		this.addresses = new ArrayList<WMEAddress>(3);
		this.varNameMap = new HashMap<String, WMEAddress>(3);
	}
	
	/**
	 * @return The list of WMEAddresses representing individual (<x> foo.bar <y>) reference commands
	 */
	public List<WMEAddress> getAddresses() { return addresses; }
	
	/**
	 * @return The LispishObject associated with this reference
	 */
	public LispishObject getWM() { return ins_wm; }
	
	/**
	 * Convert the given address path object into a LispishObject WM tree
	 * @param address the address path to convert
	 * @return The corresponding tree structure
	 */
	public static LispishObject makeWMTree(WMEAddress address) {
		LispishObject retval = new LispishObject("S1"),
				curObj = retval;
		
		// Make each step in the path a deeper nesting of object
		for (String attr : address.getPath()) {
			curObj = curObj.addObject(attr);
		}
		
		return retval;
	}
	
	/**
	 * Add a new WME address reference to this Ref collection, such as "(<x> foo.bar <y>)"
	 * @param id The identifier (variable label) for the source of this WME address. E.g. "<x>" in "(<x> foo.bar <y>)"
	 * @param path The attribute path of this WME address. E.g. "foo.bar" in "(<x> foo.bar <y>)"
	 * @param varName The label for the value at this address. E.g. "<y>" in "(<x> foo.bar <y>)"
	 */
	public void addWMEAddress(String id, String path, String varName) {
		if (id.equals("s1")) {
			id = "S1";	// Make case-insensitive state reference
		}
		WMEAddress addr = new WMEAddress(id, path, varName);
		addresses.add(addr);
		varNameMap.put(varName, addr);
	}

	/**
	 * Get a list of indices for which WMEAddress objects have an attribute path that starts with the given attribute
	 * @param pathAttr The attribute to scan for
	 * @return The list of matching indices
	 */
	public List<Integer> getMultiAttributeIndices(String pathAttr) {
		List<Integer> retval = new ArrayList<Integer>();
		
		// Check that there are addresses
		if (addresses.size() == 0) {
			return retval;
		}
		
		// Iterate and collect
		for (int i=0; i<addresses.size(); ++i) {
			if (addresses.get(i).getPathAt(0).equals(pathAttr)) {
				retval.add(i);
			}
		}
			
		return retval;
	}
	
}
