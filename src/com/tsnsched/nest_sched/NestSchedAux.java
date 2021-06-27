package com.tsnsched.nest_sched;

import java.util.ArrayList;
import java.util.Comparator;

// This file contains the auxiliary classes to create the Nesting simulation files


// Class to represent the SendWindow of a given priority
class SendWindow {
	
	private double start, duration;
	private int priority;
	
	public SendWindow(double start, double duration, int priority) {
		this.start = start;
		this.duration = duration;
		this.priority = priority;
	}
	
	public double getDuration() { return duration; }
	public int getPriority() { return priority;	}
	public double getStart() { return start;	}
}



// Class to represent the current state of the gate in a switch port, 
// where 1 represent the gate is open and 0 closed
class BitVector {
	private char bitvector[];
	
	public BitVector(int type) {
		this.bitvector = new char[8];
		if(type==0) {
			for(int i=0;i<8;i++) {
				bitvector[i]='0';
			}
		} else {
			for(int i=0;i<8;i++) {
				bitvector[i]='1';
			}
		}
	}

	public void setBit(int index, char value) {
		bitvector[index] = value;
	}
	
	public String getBitvector() {
		String st = "";
		for(int i=0;i<8;i++) {
			st+=bitvector[i];
		}
		return st;
	}

	public String makeMirror() {
		String mirror ="";
		
		for(int i=0;i<8;i++) {
			if(bitvector[i] == '1') {
				mirror += '0';
			} else {
				mirror += '1';
			}
		}
		
		return mirror;
	}
	
}



// Class to represent the generation of a flow
class FlowEntry {
	
	private double start, size;
	private int queue, id;
	private String dest; 
	
	public FlowEntry(double start, int queue, double size, int id) {
				
		this.queue = 7 - queue;
		this.size = size - 30;
		this.id = id;		
		if(start > 0) { this.start = start - ((size*8)/1000); } else { this.start = start; }		
		if(id < 10) { this.dest = "255:0:00:00:00:0"+ Integer.toString(id); } else { this.dest = "255:0:00:00:00:"+ Integer.toString(id); }
	}
	
	public String getDest() { return dest; }
	public int getId() { return id;	}
	public int getQueue() {	return queue; }
	public double getSize() { return size;	}
	public double getStart() { return start;	}
	
}



// Class to represent a traffic generator device
class DevGen {
	
	ArrayList<FlowEntry> entries = new ArrayList<FlowEntry>();	
	double defaultCycle, cycle;
	int max;
	String name;
	
	public DevGen(String name) {
		this.name = name;
	}
	
	public int getMax() { return max; }
	public void setMax(int max) { this.max = max; }
	
	public double getCycle() { return cycle; }
	public double getDefaultCycle() { return defaultCycle; }
	public ArrayList<FlowEntry> getEntries() { return entries; }
	public String getName() { return name; }
	
	public void setCycle(double cycle) { this.cycle = cycle; }
	public void setDefaultCycle(double defaultCycle) { this.defaultCycle = defaultCycle; }
	
	
	public void addEntry(double start, int queue, double size, int id) {
		FlowEntry newEntry = new FlowEntry(start, queue, size, id);
		
		entries.add(newEntry);
	}
	
}



// Sort the send windows by the start parameter
class SortWindows implements Comparator<SendWindow> 
{ 
    // Used for sorting in ascending order
    public int compare(SendWindow a, SendWindow b) 
    { 
        return Double.compare(a.getStart(), b.getStart()); 
    } 
} 


//Sort the device entries by the start parameter
class SortEntries implements Comparator<FlowEntry> 
{ 
 // Used for sorting in ascending order
 public int compare(FlowEntry a, FlowEntry b) 
 { 
     return Double.compare(a.getStart(), b.getStart()); 
 } 
} 