package schedule_generator;

import java.util.ArrayList;

import com.microsoft.z3.*;

/**
 * [Class]: FlowFragment
 * [Usage]: This class is used to represent a fragment of a flow.
 * Simply put, a flow fragment is represents the flow it belongs to
 * regarding a specific switch in the path. With this approach,
 * a flow, regardless of its type, can be broken into flow fragments
 * and distributed to the switches in the network. It holds the time 
 * values of the departure time, arrival time and scheduled time of 
 * packets from this flow on the switch it belongs to.
 * 
 */
public class FlowFragment extends Flow {
	private Flow parent;
    private RealExpr packetSize;
    private RealExpr packetPeriodicityZ3;
    private ArrayList<RealExpr> departureTimeZ3 = new ArrayList<RealExpr>();
    private IntExpr flowPriority;
    private String nodeName;
    private String nextHopName;
    private int numOfPacketsSent = Network.PACKETUPPERBOUNDRANGE;
    
    private ArrayList<Float> departureTime = new ArrayList<Float>();
    private ArrayList<Float> arrivalTime = new ArrayList<Float>();
    private ArrayList<Float> scheduledTime = new ArrayList<Float>();

    /**
     * [Method]: FlowFragment
     * [Usage]: Overloaded constructor method of this class. Receives a
     * flow as a parameter so it can retrieve properties of the flows 
     * that it belongs to.
     * 
     * @param parent    Flow object to whom this fragment belongs to
     */
    public FlowFragment(Flow parent) {
    	this.setParent(parent);
    	
        /*
         * Every time this constructor is called, the parent is also called
         * making instanceCounter++, even though it counts only the number
         * of flows.
         */
        Flow.instanceCounter--; 
        
        /*
         * Since the pathing methods for unicast and publish subscribe flows
         * are different and the size of the path makes a difference on the name,
         * there must be a type check before assigning the names
         */
        
        if(parent.getType() == Flow.UNICAST) {
            this.name = parent.getName() + "Fragment" + (parent.getFlowFragments().size() + 1);
        } else if (parent.getType() == Flow.PUBLISH_SUBSCRIBE) {
            this.name = parent.getName() + "Fragment" + (parent.pathTreeCount + 1);
            parent.pathTreeCount++;
        } else {
            // Throw error
        }
                
    }
    
    /*
     * GETTERS AND SETTERS
     */
    
    public String getName() {
        return this.name;
    }
    
    public RealExpr getDepartureTimeZ3(int index) {
        return departureTimeZ3.get(index);
    }
    
    public void setDepartureTimeZ3(RealExpr dTimeZ3, int index) {
        this.departureTimeZ3.set(index, dTimeZ3);
    }
    
    public void addDepartureTimeZ3(RealExpr dTimeZ3) {
        this.departureTimeZ3.add(dTimeZ3);
    }
    
    public RealExpr getPacketPeriodicityZ3() {
        return packetPeriodicityZ3;
    }
    
    public void setPacketPeriodicityZ3(RealExpr packetPeriodicity) {
        this.packetPeriodicityZ3 = packetPeriodicity;
    }
    
    public RealExpr getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(RealExpr packetSize) {
        this.packetSize = packetSize;
    }
    
    public IntExpr getFlowPriority() {
        return flowPriority;
    }

    public void setFlowPriority(IntExpr flowPriority) {
        this.flowPriority = flowPriority;
    }
    
    
    public void addDepartureTime(float val) {
        departureTime.add(val);
    }
    
    public float getDepartureTime(int index) {
        return departureTime.get(index);
    }
    
    public ArrayList<Float> getDepartureTimeList() {
        return departureTime;
    }
    
    public void addArrivalTime(float val) {
        arrivalTime.add(val);
    }
    
    public float getArrivalTime(int index) {
        return arrivalTime.get(index);
    }
    
    public ArrayList<Float> getArrivalTimeList() {
        return arrivalTime;
    }
    
    public void addScheduledTime(float val) {
        scheduledTime.add(val);
    }
    
    public float getScheduledTime(int index) {
        return scheduledTime.get(index);
    }
    
    public ArrayList<Float> getScheduledTimeList() {
        return scheduledTime;
    }
    
    public String getNextHop() {
        return nextHopName;
    }

    public void setNextHop(String nextHop) {
        this.nextHopName = nextHop;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
    
    public int getNumOfPacketsSent() {
        return numOfPacketsSent;
    }

    public void setNumOfPacketsSent(int numOfPacketsSent) {
        this.numOfPacketsSent = numOfPacketsSent;
    }

	public Flow getParent() {
		return parent;
	}

	public void setParent(Flow parent) {
		this.parent = parent;
	}
}
