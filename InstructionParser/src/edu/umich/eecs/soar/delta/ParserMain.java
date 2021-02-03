package edu.umich.eecs.soar.delta;

public class ParserMain {

	public static void main(String[] args) {
		// Basic test of parser
		
		DeltaParser parser = new DeltaParser();
		parser.convert_file("test_instructions01.delta", "test_instructions01.soar");

	}

}
