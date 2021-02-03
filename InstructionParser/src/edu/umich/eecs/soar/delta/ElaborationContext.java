package edu.umich.eecs.soar.delta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElaborationContext {

	private ContextMemory contextMem;
	private String name;
	private List<Rule> rules;
	private LispishObject wm;
	private List<WMERef> ruleRefs;
	
	public ElaborationContext(ContextMemory container, String name) {
		this.contextMem = container;
		this.name = name;
		this.rules = new ArrayList<Rule>();
		this.wm = null;								// The tree of potential WM paths to keep referenced for this context
		this.ruleRefs = new ArrayList<WMERef>();	// The list of Refs for each rule instructed in this context
		
		// Make default WMERefs
		/*stateRef = new WMERef(new WMEAddress("state"),"state");
		constRef = new WMERef(new WMEAddress("const"),"const");*/
	}
	
	public String getName() { return name; }
	public Rule getRule(int index) { return rules.get(index); }
	public List<Rule> getRules() { return rules; }
	
	public void addRule(Rule rule) { rules.add(rule); }
	
	public void addRef(WMERef ref) { ruleRefs.add(ref); }
	
	/**
	 * Compile the WMERefs from all instructed rules for this context into a single LispishObject WME graph.
	 * This effectively performs a graph merge for each graph of reference WMEs for each rule.
	 * @return The compiled graph object.
	 */
	public LispishObject compileRefs() {
		wm = new LispishObject("S1");
		
		// Track multi-attribute references with their magnitudes for each set of instructions
		//Map<String, Integer> frontAttrToCount = new HashMap<String, Integer>(); // A map of attributes that are leaf connectors on the tree, with the count > 1 if multi-attributes

		// Make a map for each instruction set, mapping var labels to objects in the tree
		List<Map<String, LispishObject>> varToObj = new ArrayList<Map<String, LispishObject>>(ruleRefs.size()); 
		for (int i=0; i<ruleRefs.size(); ++i) {
			HashMap<String, LispishObject> newMap = new HashMap<String, LispishObject>();
			newMap.put("S1", wm); // Start each rooted in the same S1
			varToObj.add(newMap); 
		}
		
		// Go through each list of addresses from each set of instructions iteratively growing the main tree
		boolean moreTodo = true;
		List<ArrayList<Boolean>> processed = new ArrayList<ArrayList<Boolean>>();
		for (WMERef r : ruleRefs) {
			processed.add(new ArrayList<Boolean>(Collections.nCopies(r.getAddresses().size(), false)));
		}
		
		while (moreTodo) {
			moreTodo = false;
			
			// For each set of instructions:
			for (int i=0; i<ruleRefs.size(); ++i) {
				WMERef ref = ruleRefs.get(i);

				// Scan for addresses that begin with a known reference location
				for (int j=0; j<ref.getAddresses().size(); ++j) {
					// Skip elements that have already been processed here
					if (processed.get(i).get(j))
						continue;

					WMEAddress thisAddr = ref.getAddresses().get(j);

					// If the reference is known for this instruction
					LispishObject curObj = varToObj.get(i).get(thisAddr.getIDName());
					if (curObj != null) {
						// Mark that there is more to do
						moreTodo = true;
						
						// Get the number of child branches off the main tree ref that already have this attribute name
						List<Integer> mainAttrChildren = curObj.getNamedSublistIndices(thisAddr.getPathAt(0));
						// Get the indexes of the child branches off the local instruction ref that have this attribute name
						List<Integer> lclAttrChildren = ref.getMultiAttributeIndices(thisAddr.getPathAt(0));
						
						// TODO: This graph combination could be made efficient by ensuring common multi-attr paths overlap.
						// 		 As it is, this approach blindly attaches the local tree under the first available main branch.
						//		 This might expand the smem size and thus Rete matching cost in PROPs, but shouldn't impair functionality.
						
						// For each (multi)attribute shared by both the main tree and this instruction
						for (int b=0; b < mainAttrChildren.size() && b < lclAttrChildren.size(); ++b) {
							// If necessary, append each attribute segment of this ref address path to the main tree at the parallel index of its multi-attribute branches
							WMEAddress thisAddr2 = ref.getAddresses().get(lclAttrChildren.get(b));
							
							// For each segment of this WMEAddress path:
							LispishObject curObjB = curObj;
							for (String s : thisAddr2.getPath()) {
								// Test if this path segment is already in the main tree
								List<Integer> mainSubChildren = curObjB.getNamedSublistIndices(s);
								if (mainSubChildren.size() == 0) {
									// This path element is new, add it
									curObjB = curObjB.addObject(s);
								}
								else {
									// This path attribute already exists, link to the first available instance (arbitrary for now; TODO)
									curObjB = curObjB.get(mainSubChildren.get(0));
								}
							}

							// Remember the var label referenced at the end of this branch
							varToObj.get(i).put(thisAddr2.getVarName(), curObjB);
							
							// Mark this local multi-attribute branch as processed
							processed.get(i).set(lclAttrChildren.get(b), true);
						}
						// Add any needed branches to the main tree to fit remaining local branches
						for (int b=mainAttrChildren.size(); b<lclAttrChildren.size(); ++b) {
							WMEAddress thisAddr2 = ref.getAddresses().get(lclAttrChildren.get(b));
							
							// Build branches for this address off the referenced location
							LispishObject curObjB = curObj; // FIXME: This is adding to the parent instead of the child
							for (String s : thisAddr2.getPath()) {
								curObjB = curObjB.addObject(s);
							}
							
							// Remember the var label referenced at the end of this branch
							varToObj.get(i).put(thisAddr2.getVarName(), curObjB);
							
							// Mark these local branches as processed
							processed.get(i).set(lclAttrChildren.get(b), true);
						}

					}	// END if (curObj != null)
				}	// END for (WMEaddress thisAddr : ref.getAddresses)
			}	// END for (WMERef ref : ruleRefs)
		}	// END while (moreToDo)
		
		// TODO: Link each rule's conditions to the corresponding reference in the main wm tree
		compileCondPrims(varToObj);

		return wm;
	}
	
	/**
	 * Make Prim objects for each condition in this context's rules, and add them to the containing global ContextMemory.
	 * This links each rule to its Prims so that printing to final smem network is possible.
	 * @param varToObj A map from variable names used within conditions to the corresponding main wm tree structure objects. 
	 */
	private boolean compileCondPrims(List<Map<String, LispishObject>> varToObj) {
		// Go through each rule in this context
		for (int r=0; r<rules.size(); ++r) {
			Rule rule = rules.get(r);
			// Compile each Prim for each condition in this rule
			for (Condition c : rule.getConditions()) {
				// Define the first arg id
				LispishObject id1 = varToObj.get(r).get(c.idvar1);
				
				// Error check
				if (id1 == null) {
					System.err.println("ERROR: Variable missing reference: '" + c.idvar1 + "' in rule " + rule.getName());
					return false;
				}
				
				LispishObject id2 = null;
				// Define the second arg id if there is one
				if (c.idvar2 != null) {
					id2 = varToObj.get(r).get(c.idvar2);

					// Error check
					if (id2 == null) {
						System.err.println("ERROR: Variable missing reference: '" + c.idvar2 + "' in rule " + rule.getName());
						return false;
					}
				}
				
				// Create the Prim and add it to the rule and the global ContextMemory
				Prim pnew = new Prim(c.command, id1, c.attribute1, id2, c.attribute2);
				rule.addCondPrim(pnew);
				contextMem.addPrim(pnew);

			}
		}
		
		return true;
	}
	
	public String toSmemString() {
		String retval = "(<elab-context-" + this.hashCode() + "> ^elab-context-name |" + name + "|";
		
		// Print rule instructions
		for (Rule rule : rules) {
			retval += "\r\n\t^delta <drule-" + rule.hashCode() + "> ";
		}
		retval += ") \r\n\r\n";
		
		// Print wm tree
		retval += "### WM-SPACE ###\r\n";
		retval += wm.toSmemString();
		
		return retval;
	}
}
