package edu.umich.eecs.soar.deltautil;

import java.util.List;

public class ScheduledInput {
	public long msecMoment = -1;
	public List<String> inputs;
	
	public ScheduledInput(long moment, List<String> inputs) {
		this.msecMoment = moment;
		this.inputs = inputs;
	}
	
	public long getMoment() {return msecMoment;}
}
