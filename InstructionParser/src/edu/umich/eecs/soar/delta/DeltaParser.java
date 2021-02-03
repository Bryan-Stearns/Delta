package edu.umich.eecs.soar.delta;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class DeltaParser {

	/**
	 * Read the given delta instructions file and create the corresponding soar output file. 
	 * If the output file doesn't exist, it will be created. If it does exist it will be overwritten.
	 * The output file will be a Soar file that will add the parsed delta instructions to an agent's smem. 
	 * @param infile The name of the file to open and parse
	 * @param outfile The name of the file to send output to.
	 * @return Success status
	 */
	public boolean convert_file(String infile, String outfile) {
		Path inPath = Paths.get(infile);
		Path outPath = Paths.get(outfile);
		
		// Open or overwrite output file for writing and write output header data
		if (!init_output_file(outPath)) { return false; }
		
		LispishObject fileTextStructs = new LispishObject();
		ContextMemory parsedIns = new ContextMemory();
			
		// If reading an problem-space instruction, parse it and send to output
		if (!read_lispish_file(inPath, fileTextStructs)) { return false; }
		if (!parse_instructions(fileTextStructs, parsedIns)) { return false; }
		if (!write_parsed_elab_context(outPath, parsedIns)) { return false; }
		
		// Write output file footers and return
		if (!finish_output_file(outPath)) { return false; }
		
		System.out.println("Done!");
		
		return true;
	}
	
	/**
	 * Read the contents of file with parentheses-separate text object.
	 * Return the read lines in the given list, organized into a LispishObject hierarchy.
	 * The given list will be cleared before contents are written to it.
	 * @param inPath The path of the file to read from
	 * @param ret_insLines A returned LispishObject containing the hierarchy of parenthetical blocks in the file.
	 * @return Success status
	 */
	public boolean read_lispish_file(Path inPath, LispishObject ret_insLines) {
		if (ret_insLines == null)
			return false;
		ret_insLines.clear();
		
		// Init the object with the command to load instructions (for formatting consistency: each LispishObject starts with a command)
		ret_insLines.addString("instruct-elabs");
		
		LispishObject curObj = ret_insLines; 
		
		try (Scanner scanner = new Scanner(inPath.toFile())) {
			scanner.useDelimiter("[\\p{javaWhitespace}]+|(?<=\\()|(?=\\))"); // Delimit by whitespace or parentheses
			
			// Scan through all tokens in the file and organize as LispishObjects
			while (scanner.hasNext()) {
				
				String token = scanner.next();
				
				if (token.equals("(")) {
					// Start a new object
					curObj = curObj.addObject();
				}
				else if (token.equals(")")) {
					// End current object
					curObj = curObj.getParent();
				}
				else if (token.startsWith("\"")) {
					// Start a new object that includes this String
					if (!token.endsWith("\"")) {
						scanner.useDelimiter("\"");
						token += scanner.next();
					}
					scanner.useDelimiter("[\\p{javaWhitespace}]+|(?<=\\()|(?=\\))");
					curObj.addString(token.replaceAll("\"", ""));
				}
				else if (token.startsWith(";")) {
					// Skip comments
					scanner.nextLine();
				}
				else {
					curObj.addString(token);
				}
				
				
			} // End of while (scanner.hasNext())
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * Parse the instruction text given in the hierarchy included under the given LispishObject.
	 * Add the parsed information into the given ContextMemory structure
	 * @param insObj The source instruction text to parse
	 * @param ret_parsedIns The return ContextMemory object
	 * @return Success status
	 */
	public boolean parse_instructions(LispishObject insObj, ContextMemory ret_parsedIns) {
		return parse_instructions(insObj, ret_parsedIns, null, null);
	}
	
	/**
	 * Parse the instruction text given in the hierarchy included under the given LispishObject.
	 * Add the parsed information into the given ContextMemory structure
	 * @param insObj The source instruction text to parse
	 * @param ret_parsedIns The return ContextMemory object
	 * @param curElab The current problem-space being parsed, or null if none
	 * @param curRule The current ins rule being parsed, or null if none
	 * @return Success status
	 */
	public boolean parse_instructions(LispishObject insObj, ContextMemory ret_parsedIns, ElaborationContext curElab, Rule curRule) {
		
		// Argument error checking
		if (insObj == null || !insObj.isList()) {
			System.err.println("ERROR in parse_instructions: Cannot parse NULL.");
			System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
			System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
			return false;
		}
		if (insObj.getDataList().size() == 0) {
			System.err.println("ERROR in parse_instructions: Cannot parse LispishObject. It has no data.");
			System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
			System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
			return false;
		}
		
		// Assert that the first item is text, the command name for this text block
		String cmd = insObj.getString(0);
		
		// Parse through the rest of this object based on the command

		//// 'instruct-elabs' command (the main starting command inserted by the parser)
		if (cmd.equals("instruct-elabs")) {
			// Parse each problem-space contained in the context
			for (int i=1; i<insObj.size(); ++i) {
				if (insObj.get(i).isList()) {
					if (!parse_instructions(insObj.get(i), ret_parsedIns, curElab, curRule)) {
						return false;
					}
				}
				else {
					System.err.println("ERROR in parse_instructions: expected (problem-space ...) command, found String: '" + insObj.getString(i) + "'");
					return false;
				}
			}
		}
		
		//// 'problem-space' command
		else if (cmd.equals("problem-space")) {
			// Don't allow problem-spaces to be nested
			if (curElab != null) {
				System.err.println("ERROR in parse_instructions: Nested problem-space command.");
				System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
				return false;
			}

			// Assert that the second item is the name text for this context
			String nm = insObj.getString(1);
			curElab = new ElaborationContext(ret_parsedIns, nm);	// Wait to add this to the ContextMemory until its instructions are defined

			// Parse each instruction contained in the context
			for (int i=2; i<insObj.size(); ++i) {
				if (insObj.get(i).isList()) {
					if (!parse_instructions(insObj.get(i), ret_parsedIns, curElab, curRule)) {
						return false;
					}
				}
				else {
					System.err.println("ERROR in parse_instructions: expected (ins ...) command, found String: '" + insObj.getString(i) + "'");
					System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
					return false;
				}
			}

			// Compile the WMEAddress refs for these instructions
			curElab.compileRefs();
			
			// Success: add this completed problem-space to the ContextMemory and clear the local elab marker
			ret_parsedIns.addElabContext(curElab);
			curElab = null;
		}

		//// 'ins' command
		else if (cmd.equals("ins")) {
			// Don't allow ins's to be nested
			if (curRule != null) {
				System.err.println("ERROR in parse_instructions: Nested 'ins' command.");
				System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
				System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
				return false;
			}
			// Don't allow ins outside an problem-space
			if (curElab == null) {
				System.err.println("ERROR in parse_instructions: 'ins' command outside an problem-space.");
				System.err.println("\t Found command: " + insObj.toString());
				return false;
			}

			// Assert that the second item is the name text for this rule
			String nm = insObj.getString(1);
			curRule = new Rule(nm);
			curElab.addRule(curRule);

			// Parse the elements of this rule
			for (int i=2; i<insObj.size(); ++i) {
				if (insObj.get(i).isList()) {
					if (!parse_instructions(insObj.get(i), ret_parsedIns, curElab, curRule)) {
						return false;
					}
				}
				else {
					System.err.println("ERROR in parse_instructions: Found command '" + insObj.getString(i) + "' without arguments.");
					System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
					System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
					return false;
				}
			}

			// Success: clear the curRule marker
			curRule = null;

			return true;
		}

		//// 'ref' for a rule
		else if (cmd.equals("ref")) {
			// Get any number of WMERef descriptions as sub-objects

			if (!insObj.get(1).isList()) {
				System.err.println("ERROR in parse_instructions: Malformed ref: '" + insObj.toString() + "'");
				System.err.println("\t The correct syntax is: '(ref (<id> <attr1> <label1>) (<label1> <attr2> <label2>) ...)");
				System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
				System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
				return false;
			}

			WMERef thisRef = new WMERef();
			
			// Parse the wme address refs
			for (int i=1; i<insObj.size(); ++i) {
				if (insObj.get(i).isList()) {
					String first = insObj.get(i).get(0).toString();
					
					// Check number of args
					if (insObj.get(i).size() != 3) {
						System.err.println("ERROR in parse_instructions: Found command '" + insObj.getString(i) + "' with wrong number of arguments.");
						System.err.println("\t The correct syntax is: '(<id> attr.path <label>)");
						System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
						System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
						return false;
					}

					// Only allow wme commands inside
					if (!first.toUpperCase().equals("S1")
							&& !(first.startsWith("<") && first.endsWith(">"))) {
						System.err.println("ERROR in parse_instructions: '" + insObj.toString() + "'");
						System.err.println("\t Only 'S1' or '<var>' reference structs allowed inside (ref ...)");
						System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
						System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
						return false;
					}
					
					thisRef.addWMEAddress(first, insObj.get(i).get(1).toString(), insObj.get(i).get(2).toString());

					/*if (!parse_instructions(insObj.get(i), ret_parsedIns, curElab, curRule)) {
						return false;
					}*/
				}
				else {
					System.err.println("ERROR in parse_instructions: Found command '" + insObj.getString(i) + "' without proper arguments.");
					System.err.println("\t The correct syntax is: '(ref (<id> <attr1> <label1>) (<label1> <attr2> <label2>) ...)");
					System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
					System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
					return false;
				}
			}
			
			// Add this Ref to the context of instructions
			curElab.addRef(thisRef);
			
		}

		//// 'wme' for a rule
		/*else if (cmd.equals("wme")) {
			// Assert that the second item is the name text for this wme
			String nm = insObj.getString(1);
			// Assert that the third item is the path text for this wme
			String path = insObj.getString(2);

			// Add the ref
			WMEAddress addr = new WMEAddress(path);
			WMERef ref = new WMERef(addr, nm);
			curRule.addRef(ref);
			ret_parsedIns.addAddress(addr);
		}*/

		//// 'condition' for a rule
		else if (cmd.equals("condition")) {
			if (!insObj.get(1).isList()) {
				System.err.println("ERROR in parse_instructions: Malformed condition: '" + insObj.toString() + "'");
				System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
				System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
				return false;
			}

			// Get any number of Prim conditions
			for (int i=1; i<insObj.size(); ++i) {
				if (insObj.get(i).isList()) {
					// Only allow Prim commands inside
					String op = insObj.get(i).get(0).toString();
					if (!op.equals("==") && !op.equals("<>") && !op.equals("?") && !op.equals("-") // equality, inequality, existence, negation 
							&& !op.equals("<") && !op.equals(">") && !op.equals("<=") && !op.equals(">=")) {
						System.err.println("ERROR in parse_instructions: '" + insObj.toString() + "'");
						System.err.println("\t Only prim condition commands allowed inside (condition (...))");
						System.err.println("\t Valid commands are: '==', '<>', '?', '-', '>', '<', '<=', '>='");
						System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
						System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
						return false;
					}

					if (!parse_instructions(insObj.get(i), ret_parsedIns, curElab, curRule)) {
						return false;
					}
				}
				else {
					System.err.println("ERROR in parse_instructions: Found command '" + insObj.getString(i) + "' without proper arguments.");
					System.err.println("\t The correct syntax is: '(condition (...) (...) ... )");
					System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
					System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
					return false;
				}
			}
		}

		//// 2-arg prims
		else if (Prim.isBinaryOp(cmd)) {
			// Check format
			if (insObj.size() != 5) {
				System.err.println("ERROR in parse_instructions: Found command '" + insObj.toString() + "' without proper arguments.");
				System.err.println("\t The correct syntax is: '(" + cmd + " ref1 attr1 ref2 attr2)");
				System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
				System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
				return false;
			}

			// Assert that the second item is the WMERef name for arg1
			String nm1 = insObj.getString(1);
			// Assert that the third item is the attribute name for arg1
			String attr1 = insObj.getString(2);
			// Assert that the second item is the WMERef name for arg2
			String nm2 = insObj.getString(3);
			// Assert that the third item is the attribute name for arg2
			String attr2 = insObj.getString(4);
			
			// Check for const values in refs
			if (nm1.equals("const")) {
				attr1 = String.valueOf(curRule.addConst(attr1));	// Shouldn't ever get called if users treat first arg as WM var, but users might not do that.
			}
			if (nm2.equals("const")) {
				attr2 = String.valueOf(curRule.addConst(attr2));
			}

			// Add the condition
			Condition c = new Condition(curRule, Prim.getType(cmd), nm1, attr1, nm2, attr2);
			curRule.addCondition(c);
		}

		//// 1-arg prims
		else if (Prim.isUnaryOp(cmd)) {
			// Check format
			if (insObj.size() != 3) {
				System.err.println("ERROR in parse_instructions: Found command '" + insObj.toString() + "' without proper arguments.");
				System.err.println("\t The correct syntax is: '(" + cmd + " ref1 attr1)");
				System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
				System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
				return false;
			}

			// Assert that the second item is the WMERef name for arg1
			String nm1 = insObj.getString(1);
			// Assert that the third item is the attribute name for arg1
			String attr1 = insObj.getString(2);

			// Add the condition
			Condition c = new Condition(curRule, Prim.getType(cmd), nm1, attr1);
			curRule.addCondition(c);
		}

		//// 'action' for a rule
		else if (cmd.equals("operator")) {
			String act = insObj.getString(1);
			curRule.setAction(act);
		}

		//// 'description' for a rule
		else if (cmd.equals("description")) {
			String desc = insObj.getString(1);
			curRule.setDescription(desc);
		}

		//// Unknown command
		else {
			System.err.println("ERROR in parse_instructions: Unexpected command: " + cmd);
			System.err.println("\t Current problem-space: " + (curElab == null ? "NULL" : curElab.getName()));
			System.err.println("\t Current rule: " + (curRule == null ? "NULL" : curRule.getName()));
			return false;
		}


		return true;
	}

	/**
	 * Append the parsed contents of an problem-space to file.
	 * @param outPath The path of the file to write to
	 * @param parsedInsLines A list of the line-lists to write for each (ins) block of the source problem-space.
	 * @return Success status
	 */
	public boolean write_parsed_elab_context(Path outPath, ContextMemory parsedInsLines) {
		// TODO: write in toString friendly format
		List<String> outLines = parsedInsLines.getSmemStrings();
		
		try {
			Files.write(outPath, outLines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * Create the given output file if it doesn't exist.
	 * Overwrite it if it does.
	 * Then write the header description and smem --add opening text. 
	 * @return Success status
	 */
	public boolean init_output_file(Path outPath) {
		List<String> lines = Arrays.asList("#############################################################################",
										   "# THIS FILE TRANSLATES INSTRUCTIONS INTO SMEM FORMAT FOR A SOAR PROPS AGENT #",
										   "#############################################################################",
										   "",
										   "smem --add {",
										   "");
		try {
			Files.deleteIfExists(outPath);
			Files.write(outPath, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * If the given file exists, close off the smem --add text command.
	 * @return Success status
	 */
	public boolean finish_output_file(Path outPath) {
		List<String> lines = Arrays.asList("}", "");
		
		try {
			Files.write(outPath, lines, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
}
