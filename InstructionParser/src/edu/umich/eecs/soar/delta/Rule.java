package edu.umich.eecs.soar.delta;

import java.util.ArrayList;
import java.util.List;

public class Rule {
	
	private String name;
	private List<String> consts;
	//private List<WMERef> refs;
	private List<Condition> conditions;
	private List<Prim> condPrims;
	private String action;			// Only a single action; might refer to a context though
	private String description;
	
	public Rule(String name) {
		this.name = name;
		this.consts = new ArrayList<String>(4);
		//this.refs = new ArrayList<WMERef>(4);
		this.conditions = new ArrayList<Condition>(4);
		this.condPrims = new ArrayList<Prim>(4);
		this.action = "";
		this.description = "";
		
		// Add default WMERefs
	}
	
	//public void addRef(WMERef ref) { this.refs.add(ref); }
	public void addCondition(Condition c) { this.conditions.add(c); }
	public void addCondPrim(Prim p) { this.condPrims.add(p); }
	public void setAction(String action) { this.action = action; }
	public void setDescription(String desc) { this.description = desc; }
	
	/**
	 * Add the given String value to the list of consts for this rule.
	 * If the const is already in the list, it will not be added.
	 * @param c The value to add to the list of consts
	 * @return The index of the added value in the consts list for this rule
	 */
	public int addConst(String c) {
		int index = consts.indexOf(c);
		if (index != -1) {
			return index;
		}
		this.consts.add(c);
		return consts.size()-1;
	}
	
	public String getName() { return name; }
	public String getConst(int index) { return consts.get(index); }
	public int getConstInd(String c) { return consts.indexOf(c); }
	//public WMERef getRef(int index) { return refs.get(index); }
	public Condition getCondition(int index) { return conditions.get(index); }
	public Prim getCondPrim(int index) { return condPrims.get(index); }
	public List<Condition> getConditions() { return conditions; }
	public List<Prim> getCondPrims() { return condPrims; }
	public String getAction() { return action; }
	public String getDescription() { return description; }
	
	/**
	 * Get the WMERef object with the given name if it is known.
	 * @param label The local var name for this WME reference
	 * @return The WMERef object with the given name, if any, or null if none.
	 */
	/*public WMERef findRef(String label) {
		// TODO: Consider making a more efficient lookup structure by which to find WMERefs. Usually there will not be many refs though.
		for (WMERef ref : refs) {
			if (ref.getName().equals(label)) {
				return ref;
			}
		}
		return null;
	}*/
	
	public String toSmemString() {
		String retval = "(<drule-" + this.hashCode() + "> ^op-name |" + action + "|";
		
		// Print the link to the consts object
		if (consts != null) {
			retval += "\r\n\t^const <Q" + consts.hashCode() + "> ";
		}
		
		// Print out the conditions
		/*for (Prim p : conditions) {
			retval += "\r\n\t^prop <prop-C" + p.hashCode() + "> ";
		}
		reval += ")\r\n";*/
		
		// Print out the consts for this rule
		if (consts != null && consts.size() > 0) {
			retval += "(<Q" + consts.hashCode() + "> ";
			for (int i=0; i<consts.size(); ++i) {
				retval += "\r\n\t^" + i + " " + consts.get(i);
			}
			retval += ")\r\n";
		}
		
		// Print out the rule-specific WM refs
		/*for (WMERef ref : refs) {
			if (!ref.getName().equals("state") && !ref.getName().equals("const"))
			retval += ref.toString();
		}*/
		
		// TODO: This only covers plain elaboration context rules, not apply actions
		
		return retval;
	}
}
