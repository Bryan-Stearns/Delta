package edu.umich.eecs.soar.delta;

public class Prim {

	enum PrimType {
		// Conditions:
		EQUALITY, INEQUALITY, EXISTENCE, INEXISTENCE, TYPE_EQUALITY, 
		LESS_THAN, GREATER_THAN, LESS_EQUAL, GREATER_EQUAL, NEGATION,
		// Actions:
		ADD, REMOVE, ACCEPTABLE, INDIFFERENT, BETTER, WORSE, REQUIRE;
		
		public boolean isCondition() {
			return (this.ordinal() < ADD.ordinal());	// Assumes ADD is the first action type and actions are listed last!
		}
		
		/**
		 * Get whether this type is a binary operation.
		 * @return True if the label is a binary operation, false otherwise
		 */
		public boolean isBinaryOp() {
			switch (this) {
			case EQUALITY:
			case INEQUALITY:
			case TYPE_EQUALITY:
			case LESS_THAN:
			case GREATER_THAN:
			case LESS_EQUAL:
			case GREATER_EQUAL:
			case BETTER:
			case WORSE:
				return true;
			default:
				return false;
			}
		}
		
	}

	/**
	 * Get the enum value associated with the given string operator label.
	 * For example, given "==", the method returns PrimType.EQUALITY
	 * If no known type matches the given label, null is returned.
	 * @param op_label The String operator label to get the type for
	 * @return The associated PrimType value
	 */
	public static PrimType getType(String op_label) {
		switch (op_label) {
		case "==":
			return PrimType.EQUALITY;
		case "<>":
			return PrimType.INEQUALITY;
		case "?":
			return PrimType.EXISTENCE;
		case "!=":
			return PrimType.INEXISTENCE;
		case "<=>":
			return PrimType.TYPE_EQUALITY;
		case "<":
			return PrimType.LESS_THAN;
		case ">":
			return PrimType.GREATER_THAN;
		case "<=":
			return PrimType.LESS_EQUAL;
		case ">=":
			return PrimType.GREATER_EQUAL;
		case "-":
			return PrimType.NEGATION;
		default:
			return null;
		}
	}

	/**
	 * Get whether the given String operator label is a unary operation.
	 * For example, given "==", return false. Given "?", return true; 
	 * @param op_label The operator label 
	 * @return True if the label is a unary operation, false otherwise
	 */
	public static boolean isUnaryOp(String op_label) {
		switch (op_label) {
		case "?": case "-": case "+":
			return true;
		default:
			return false;
		}
	}

	/**
	 * Get whether the given String operator label is a binary operation.
	 * For example, given "==", return true. Given "?", return false; 
	 * @param op_label The operator label 
	 * @return True if the label is a binary operation, false otherwise
	 */
	public static boolean isBinaryOp(String op_label) {
		switch (op_label) {
		case "==":
		case "<>":
		case "!=":
		case "<=>":
		case "<":
		case ">":
		case "<=":
		case ">=":
			return true;
		default:
			return false;
		}
	}
	
	private PrimType type;
	private LispishObject address1,	// The ID reference for arg1
				   		  address2;	// The ID reference for arg2 (if there is one)
	private String attr1,			// The WME attribute of arg1 
				   attr2;			// The WME attribute of arg2 (if there is one)

	/**
	 * Define a Prim operation. If the operation does not use a second argument, use "null" for address2 and attr2.
	 * @param type The primitive operation type
	 * @param address1 The path from the state to arg1 WME ID
	 * @param attr1 The arg1 WME attribute
	 * @param address2 The path from the state to arg2 WME ID
	 * @param attr2 The arg2 WME attribute
	 */
	public Prim(PrimType type, LispishObject address1, String attr1, LispishObject address2, String attr2) {
		this.type = type;
		this.address1 = address1;
		this.attr1 = attr1;
		this.address2 = address2;
		this.attr2 = attr2;
	}
	
	public String toSmemString() {
		String AC = (type.isCondition() ? "C" : "A");
		String retval = "(<prop-" + AC + this.hashCode() + "> ^name |_P" + AC + this.hashCode() + "|";
		retval += "\r\n\t^prop-type " + type.toString().toLowerCase();
		retval += "\r\n\t^attr1 " + attr1;
		retval += "\r\n\t^address1 " + address1.getSmemVarName();
		if (address2 != null) {
			retval += "\r\n\t^attr2 " + attr2;
			retval += "\r\n\t^address2 " + address2.getSmemVarName();
		}
		
		retval += ") \r\n";
		
		return retval;
	}

}
