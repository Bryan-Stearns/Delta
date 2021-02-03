package edu.umich.eecs.soar.delta;

import java.util.ArrayList;
import java.util.List;

public class ElaborationContext {

	private String name;
	private List<Rule> rules;
	private WMERef stateRef,
				   constRef;
	
	public ElaborationContext(String name) {
		this.name = name;
		this.rules = new ArrayList<Rule>();
		
		// Make default WMERefs
		stateRef = new WMERef(new WMEAddress("state"),"state");
		constRef = new WMERef(new WMEAddress("const"),"const");
	}
	
	public String getName() { return name; }
	public Rule getRule(int index) { return rules.get(index); }
	public List<Rule> getRules() { return rules; }
	
	public void addRule(Rule rule) {
		rules.add(rule);
		// Add default refs
		rule.addRef(stateRef);
		rule.addRef(constRef);
	}
	
	@Override
	public String toString() {
		String retval = "(<elab-context-" + this.hashCode() + "> ^elab-context-name |" + name + "|";
		
		for (Rule rule : rules) {
			retval += "\r\n\t^delta <drule-" + rule.hashCode() + "> ";
		}
		retval += ") \r\n";
		
		return retval;
	}
}
