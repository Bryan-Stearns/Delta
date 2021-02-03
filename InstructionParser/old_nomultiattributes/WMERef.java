package edu.umich.eecs.soar.delta;

public class WMERef {

	private WMEAddress address;
	private String name;				// The var name to associate with this reference (such as "<var>" in "foo.bar <var>")
	
	/**
	 * Create a WME reference object.
	 * @param address The attribute path to this WME. For example, for "S1.foo.bar <name>", the object that represents "foo.bar".
	 * @param name The variable name for this WME. For example, for "S1.foo.bar <name>", enter "<name>".
	 */
	public WMERef(WMEAddress address, String name) {
		this.address = address;
		this.name = name;
	}
	
	/**
	 * @return The var name associated with this reference (such as "<var>" in "foo.bar <var>")
	 */
	public String getName() { return name; }
	
	/**
	 * @return The WMEAddress associated with this reference
	 */
	public WMEAddress getAddress() { return address; }
	
	@Override
	public String toString() {
		// Print the main Ref object and its links to each element of the path
		String retval = "(<Ref-" + this.hashCode() + "> ^name |" + name + "|";
		retval += "\r\n\t^address <Addr-" + address.hashCode() + ">)\r\n";
		
		return retval;
	}
}
