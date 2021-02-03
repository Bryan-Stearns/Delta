package edu.umich.eecs.soar.delta;

import java.util.Arrays;
import java.util.List;

/**
 * This class represents the parsed content of a ref sub-command such as (s1 foo.bar <label>)
 * @author bryan
 *
 */
public class WMEAddress {

	private List<String> attr_path;
	private String varName;
	
	/**
	 * Create a WME address object.
	 * @param path The attribute path to this WME. For example, for "S1.foo.bar <name>", enter "foo.bar".
	 */
	public WMEAddress(String path) {
		if (path.equals("state")) {
			attr_path = null;
			varName = "props$rootstate";
		}
		else if (path.equals("const")) {
			attr_path = null;
			varName = "props$const";
		}
		else {
			attr_path = Arrays.asList(path.split("\\."));
			varName = "<Addr-" + this.hashCode() + ">";
		}
	}
	
	/**
	 * Get the attribute path element at the given index.
	 * For example, for a path "foo.bar", get(1) would return "bar".
	 * @param index The index of the element to get
	 * @return The String name of the attribute at the given index.
	 */
	public String get(int index) { return attr_path.get(index); }
	
	/**
	 * Get the String of the attribute path from S1 contained in this WMERef.
	 * Ex: for a WME "foo.bar <wme>", the returned String would be "foo.bar"
	 * @return The String of the attribute path
	 */
	public String getPath() { return String.join(".", attr_path); }
	
	/**
	 * @return The "<...>" label for this address reference for printing in the smem --add file.
	 */
	public String getVarName() {
		return varName;
	}
	
	@Override
	public String toString() {
		// Don't print if only the state or const keyword
		if (attr_path == null)
			return "";
		
		String hash = String.valueOf(this.hashCode());
		
		// Print the main Ref object and its links to each element of the path
		String retval = "(" + varName + " ^size " + attr_path.size();
		for (int i=1; i<=attr_path.size(); ++i) {
			retval += "\r\n\t^step <AddrStep-" + hash + "-" + i + ">";
		}
		retval += "\r\n\t^final <AddrStep-" + hash + "-" + attr_path.size() + ">"; // Note which link is the end of the chain
		retval += ")\r\n";
		
		// Print the objects for each element of the path
		String prevID = "props$rootstate";
		for (int i=1; i<=attr_path.size(); ++i) {
			retval += "(<AddrStep-" + hash + "-" + i + "> ^prev " + prevID;
			retval += "\r\n\t^attribute " + attr_path.get(i-1);
			retval += ")\r\n";
			prevID = "<AddrStep-" + hash + "-" + i + ">";
		}

		return retval;
	}
}
