package edu.umich.eecs.soar.delta;

/**
 * A simple container struct to hold the instruction String condition tokens
 * @author bryan
 */
public class Condition {
	public Prim.PrimType command;
	public String attribute1,
				   attribute2,
				   idvar1,
				   idvar2;
	public Rule rule;

	public Condition(Rule rule, Prim.PrimType cmd, String idvar1, String attr1) {
		this(rule, cmd, idvar1, attr1, null, null);
	}
	public Condition(Rule rule, Prim.PrimType cmd, String idvar1, String attr1, String idvar2, String attr2) {
		this.rule = rule;
		this.command = cmd;
		this.idvar1 = idvar1;
		this.idvar2 = idvar2;
		this.attribute1 = attr1;
		this.attribute2 = attr2;
	}
	
	@Override
	public String toString() {
		String retval = "( " + command.toString() + " " + idvar1 + " " + attribute1;
		if (command.isBinaryOp()) {
			if (idvar2 != null) {
				retval += " " + idvar2;
			}
			else {
				retval += " ERROR";
			}
			
			if (attribute2 != null) {
				retval += " " + attribute2; 
			}
			else {
				retval += " ERROR";
			}
		}
		retval += " )";
		
		return retval;
	}
	
}
