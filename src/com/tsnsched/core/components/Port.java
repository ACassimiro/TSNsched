package com.tsnsched.core.components;
//TSNsched uses the Z3 theorem solver to generate traffic schedules for Time Sensitive Networking (TSN)
//
//    Copyright (C) 2021  Aellison Cassimiro
//    
//    TSNsched is licensed under the GNU GPL version 3 or later:
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//    
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <https://www.gnu.org/licenses/>.


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import com.tsnsched.core.network.Network;

/**
 * [Class]: Port
 * [Usage]: This class is used to implement the logical role of a
 * port of a switch for the schedule. The core of the scheduling 
 * process happens here. Simplifying the whole process, the other
 * classes in this project are used to create, manage and break 
 * flows into smaller pieces. These pieces are given to the switches,
 * and from the switches they will be given to their respective ports.
 * 
 * After this is done, each port now has an array of fragments of flows
 * that are going through them. This way, it is easier to schedule
 * the packets since all you have to focus are the flow fragments that
 * might conflict in this specific port. The type of flow, its path or
 * anything else does not matter at this point.
 */
public class Port implements Serializable {
	
	private Boolean isModifiedOrCreated = true;
	
	private static final long serialVersionUID = 1L;
	private Boolean useMicroCycles = false;
    private Boolean useHyperCycle = false;
    
    private double interframeGapSize = 12;
    
    private ArrayList<Double> listOfPeriods = new ArrayList<Double>();
    private double definedHyperCycleSize = -1;
    private double microCycleSize = -1; 
    
	private String name;
    private String connectsTo;
    
    private double bestEffortPercent = 0.5f;
    transient RealExpr bestEffortPercentZ3;
    
    private Cycle cycle;
    private ArrayList<FlowFragment> flowFragments;
    private int packetUpperBoundRange = Network.PACKETUPPERBOUNDRANGE; // Limits the applications of rules to the packets
    private int cycleUpperBoundRange = Network.CYCLEUPPERBOUNDRANGE; // Limits the applications of rules to the cycles

	private double gbSize;
	
	protected double maxPacketSize;
    protected double timeToTravel;
    protected double transmissionTime;
    protected double portSpeed;
   
	protected int portNum;

	private transient RealExpr gbSizeZ3; // Size of the guardBand
    protected transient RealExpr maxPacketSizeZ3;
    protected transient RealExpr timeToTravelZ3;
    protected transient RealExpr transmissionTimeZ3;
    protected transient RealExpr portSpeedZ3;
    protected transient RealExpr interframeGapSizeZ3;
    
    
    /**
     * [Method]: Port   
     * [Usage]: Overloaded constructor of this class. Will start 
     * the port with setting properties given as parameters.
     * 
     * @param name                  Logical index of the port for z3
     * @param maxPacketSize         Maximum size of a packet that can be transmitted by this port
     * @param timeToTravel          Time to travel on the middle used by this port
     * @param transmissionTime      Time taken to process a packet in this port
     * @param portSpeed             Transmission speed of this port
     * @param gbSize                Size of the guard band
     * @param cycle                 Cycle used by the port
     */
    public Port (String name,
    		int portNum,
            String connectsTo,
            double maxPacketSize,
            double timeToTravel,
            double transmissionTime,
            double portSpeed,
            double gbSize,
            Cycle cycle) {
        this.name = name;
        this.portNum = portNum;
        this.connectsTo = connectsTo;
        this.maxPacketSize = maxPacketSize;
        this.timeToTravel = timeToTravel;
        this.transmissionTime = transmissionTime;
        this.portSpeed = portSpeed;
        this.gbSize = gbSize;
        this.cycle = cycle;
        this.flowFragments = new ArrayList<FlowFragment>();
        this.cycle.setPortName(this.name);
    }
    

    /**
     * [Method]: toZ3
     * [Usage]: After setting all the numeric input values of the class,
     * generates the z3 equivalent of these values and creates any extra
     * variable needed.
     * 
     * @param ctx      Context variable containing the z3 environment used
     */
    public void toZ3(Context ctx) {
        this.maxPacketSizeZ3 = ctx.mkReal(Double.toString(this.maxPacketSize));
        this.timeToTravelZ3 = ctx.mkReal(Double.toString(this.timeToTravel));
        this.transmissionTimeZ3 = ctx.mkReal(Double.toString(this.transmissionTime));
        this.portSpeedZ3 = ctx.mkReal(Double.toString(portSpeed));
        this.bestEffortPercentZ3 = ctx.mkReal(Double.toString(bestEffortPercent));
        this.interframeGapSizeZ3 = ctx.mkReal(Double.toString(this.interframeGapSize));
        this.gbSizeZ3 = ctx.mkRealConst(this.name + "guardBand");
        
        if(this.cycle.getFirstCycleStartZ3() == null) {
        	this.cycle.toZ3(ctx);
        }
    }

    
    /**
     * [Method]: setUpCycleRules
     * [Usage]: This method is responsible for setting up the scheduling rules related
     * to the cycle of this port. Assertions about how the time slots are supposed to be
     * are also specified here.
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     */
    private void setUpCycleRules(Solver solver, Context ctx) {
    	
    	solver.add(
			ctx.mkEq(
				this.gbSizeZ3, 
				ctx.mkDiv(
					ctx.mkReal(Double.toString(this.gbSize)),
					this.portSpeedZ3
				)
			)
		);

        for(int numericFlowPriority = 0; numericFlowPriority < this.cycle.getNumOfPrts(); numericFlowPriority++) {
        	for(int index = 0; index < this.cycle.getNumOfSlots(numericFlowPriority); index++) {
                IntExpr flowPriority = ctx.mkInt(numericFlowPriority);
                IntExpr indexZ3 = ctx.mkInt(index);
                
                // A slot will be somewhere between 0 and the end of the cycle minus its duration (Slot in cycle constraint)
                solver.add(ctx.mkGe(cycle.slotStartZ3(ctx, flowPriority, indexZ3), ctx.mkInt(0)));
                solver.add(
                    ctx.mkLe(cycle.slotStartZ3(ctx, flowPriority, indexZ3), 
                        ctx.mkSub(
                            cycle.getCycleDurationZ3(),
                            cycle.slotDurationZ3(ctx, flowPriority, indexZ3)
                        )
                    )
                );
                 
                // Every slot duration is greater or equal 0 and lower or equal than the maximum (Slot duration constraint)
                solver.add(ctx.mkGe(cycle.slotDurationZ3(ctx, flowPriority, indexZ3), ctx.mkInt(0)));
                solver.add(ctx.mkLe(cycle.slotDurationZ3(ctx, flowPriority, indexZ3), cycle.getMaximumSlotDurationZ3()));
                
                //Every slot must fit inside a cycle
                solver.add(
                    ctx.mkGe(
                        cycle.getCycleDurationZ3(), 
                        ctx.mkAdd(
                            cycle.slotStartZ3(ctx, flowPriority, indexZ3), 
                            cycle.slotDurationZ3(ctx, flowPriority, indexZ3)
                        )
                    )
                );
                
                /*
                 * If the priority of the fragments are the same, then the start and duration
                 * of a slot is also the same (needs to be specified due to z3 variable naming
                 * properties) (Same priority, same slot constraint)
                
                for (FlowFragment auxFrag : this.flowFragments) {
                    solver.add(
                        ctx.mkImplies(
                            ctx.mkEq(frag.getFragmentPriorityZ3(), auxFrag.getFragmentPriorityZ3()), 
                            ctx.mkAnd(
                                    
                                 ctx.mkEq(
                                         cycle.slotStartZ3(ctx, frag.getFragmentPriorityZ3(), indexZ3),
                                         cycle.slotStartZ3(ctx, auxFrag.getFragmentPriorityZ3(), indexZ3)
                                 ),
                                 ctx.mkEq(
                                         cycle.slotDurationZ3(ctx, frag.getFragmentPriorityZ3(), indexZ3),
                                         cycle.slotDurationZ3(ctx, auxFrag.getFragmentPriorityZ3(), indexZ3)
                                 )

                            )    
                        )
                    );
                }
                 */
            
                // No two slots can overlap (No overlapping slots constraint)
                for(int auxNumericFlowPriority = 0; auxNumericFlowPriority < this.cycle.getNumOfPrts(); auxNumericFlowPriority++) {
                    if(auxNumericFlowPriority == numericFlowPriority) {
                        continue;
                    }
                	for(int auxIndex = 0; auxIndex < this.cycle.getNumOfSlots(numericFlowPriority); auxIndex++) {

                        IntExpr auxIndexZ3 = ctx.mkInt(auxIndex);
                        
	                    
	                    IntExpr auxFlowPriority = ctx.mkInt(auxNumericFlowPriority);
	                    
	                    solver.add(
	                        ctx.mkImplies(
	                            ctx.mkNot(
	                                ctx.mkEq(
	                                    flowPriority,
	                                    auxFlowPriority
	                                )
	                            ),
	                            ctx.mkOr(
	                                ctx.mkGe(
	                                    cycle.slotStartZ3(ctx, flowPriority, indexZ3),
	                                    ctx.mkAdd(
	                                        cycle.slotStartZ3(ctx, auxFlowPriority, auxIndexZ3),
	                                        cycle.slotDurationZ3(ctx, auxFlowPriority, auxIndexZ3)
	                                    )
	                                ), 
	                                ctx.mkLe(
	                                    ctx.mkAdd(
	                                        cycle.slotStartZ3(ctx, flowPriority, indexZ3),
	                                        cycle.slotDurationZ3(ctx, flowPriority, indexZ3)
	                                    ),
	                                    cycle.slotStartZ3(ctx, auxFlowPriority, auxIndexZ3)
	                                )
	                            )
	                        )
	                    );
                	
                	}
            	}
                
                
                if(index < this.cycle.getNumOfSlots(numericFlowPriority) - 1) {
                	solver.add(
            			ctx.mkLe( 
        					ctx.mkAdd(
    							cycle.slotStartZ3(ctx, flowPriority, indexZ3),   
    							cycle.slotDurationZ3(ctx, flowPriority, indexZ3)
							),
    						cycle.slotStartZ3(ctx, flowPriority, ctx.mkInt(index + 1))
    					)
        			);
                }


                
                /*
                 * If 2 slots are not consecutive, then there must be a space
                 * of at least gbSize (the size of the guard band) between them
                 * (guard band constraint).
                 */
                	for(int prt = 0; prt < this.cycle.getNumOfPrts(); prt ++) {
                		for(int auxIndex = 0; auxIndex < this.cycle.getNumOfSlots(prt); auxIndex++) {
                        	IntExpr auxIndexZ3 = ctx.mkInt(auxIndex);
                        	IntExpr auxFlowPriority = ctx.mkInt(prt);
                        	
                        	solver.add(
                    			ctx.mkImplies(
                					ctx.mkAnd(
            							ctx.mkNot(
    											ctx.mkEq(auxFlowPriority, flowPriority)
    									),
            							ctx.mkNot(
        									ctx.mkEq(
    											cycle.slotStartZ3(ctx, flowPriority, indexZ3), 
    											ctx.mkAdd(
    												cycle.slotDurationZ3(ctx, auxFlowPriority, auxIndexZ3),
    												cycle.slotStartZ3(ctx, auxFlowPriority, auxIndexZ3)
    											)                                                
    										)
    									),
            							ctx.mkGt(
        									cycle.slotStartZ3(ctx, flowPriority, indexZ3), 
        									cycle.slotStartZ3(ctx, auxFlowPriority, auxIndexZ3)
    									)
        							),
                					ctx.mkGe(
            							cycle.slotStartZ3(ctx, flowPriority, indexZ3),
            							ctx.mkAdd(
        									cycle.slotStartZ3(ctx, auxFlowPriority, auxIndexZ3),
        									cycle.slotDurationZ3(ctx, auxFlowPriority, auxIndexZ3),
        									gbSizeZ3
    									)   
        							)                        
            					)
                			);
                        }
                	}                    
                
                /**/
        	}
        	
        }
        
    }
    
    /**
     * [Method]: setupTimeSlots
     * [Usage]: Given a single flow fragment, establish the scheduling rules 
     * regarding its proper slot, referencing it using the fragment's priority.
     * 
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param flowFrag      A fragment of a flow that goes through this port
     */
    private void setupTimeSlots(Solver solver, Context ctx, FlowFragment flowFrag) {
    	IntExpr indexZ3;
    	
    	// If there is a flow assigned to the slot, slotDuration must be greater than transmission time
    	for(int prt = 0; prt<this.cycle.getNumOfPrts(); prt++) {
    		for(int index = 0; index < this.cycle.getNumOfSlots(prt); index++) {
        		solver.add(
    				ctx.mkImplies(
    					ctx.mkEq(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(prt)), 
						ctx.mkGe(
	    					cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), ctx.mkInt(index+1)), 
	    					ctx.mkAdd(
    							cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), ctx.mkInt(index)),
    							cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), ctx.mkInt(index))
	    					)
	    				)
					)
    			);
        	}
    	}
    	

    	for(int prt = 0; prt<this.cycle.getNumOfPrts(); prt++) {
	    	for(int index = 0; index < this.cycle.getNumOfSlots(prt); index++) {
	    		indexZ3 = ctx.mkInt(index);
	    		
		        // solver.add(ctx.mkGe(cycle.slotDurationZ3(ctx, flowFrag.getFlowPriority(), indexZ3), this.transmissionTimeZ3));
		        
		        // Every flow must have a priority (Priority assignment constraint)
		        solver.add(ctx.mkGe(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(0))); 
		        solver.add(ctx.mkLt(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(this.cycle.getNumOfPrts())));
		        
		        // Slot start must be <= cycle time - slot duration 
		        solver.add(
	        		ctx.mkImplies(
        				ctx.mkEq(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(prt)), 
			            ctx.mkLe(
			                ctx.mkAdd(
			                    cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
			                    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3)
			                ), 
			                cycle.getCycleDurationZ3()
			            )
		            )
	        		
		        );
		    	
	    	}
    	}
    }
    
    /**
     * [Method]: setupDevPacketTimes
     * [Usage]: Sets the core scheduling rules for a certain number of
     * packets of a flow fragment. The number of packets is specified 
     * by the packetUpperBound range variable. The scheduler will attempt
     * to fit these packets within a certain number of cycles, specified
     * by the cycleUpperBoundRange variable.
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param flowFrag      A fragment of a flow that goes through this port
     */
    private void setupDevPacketTimes(Solver solver, Context ctx, FlowFragment flowFrag) {

        // For the specified range of packets defined by [0, upperBoundRange],
        // apply the scheduling rules.
        for(int i = 0; i < flowFrag.getNumOfPacketsSent(); i++) {
            // Make t3 > t2 + transmissionTime
        	
        	/*
        	System.out.println("Attempting to set rules on " + flowFrag.getName());
        	System.out.println("Making " + this.scheduledTime(ctx, i, flowFrag) 
        						+ " greater or equals to " + this.arrivalTime(ctx, i, flowFrag)
        						+ " plus " + ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3));
        	System.out.println(flowFrag.getPacketPeriodicityZ3());
        	System.out.println(this.arrivalTime(ctx, i, flowFrag));
        	System.out.println(this.portSpeedZ3);
        	System.out.println(flowFrag.getPacketSizeZ3());
        	/**/
        	
            solver.add( // Time to Transmit constraint.
                ctx.mkGe(
                    this.scheduledTime(ctx, i, flowFrag),
                    ctx.mkAdd(this.arrivalTime(ctx, i, flowFrag), ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3))
                )
            );
            
        }
        
        IntExpr indexZ3 = null;
        Expr auxExp = null;
        Expr auxExp2 = ctx.mkTrue();
        Expr exp = null;

        for(int prt = 0; prt<this.cycle.getNumOfPrts(); prt++) {
	        for(FlowFragment auxFragment : this.flowFragments) {
	        	
	        	/*
	        	System.out.println("Num de pacotes escalonados:" + auxFragment.getNumOfPacketsSent());
	        	
	        	for(int i = 0; i < auxFragment.getNumOfPacketsSent(); i++) {
	            	solver.add(
	        			ctx.mkEq(
	        				this.arrivalTime(ctx, i, auxFragment), 
							ctx.mkAdd(
									this.departureTime(ctx, i, auxFragment),
									this.timeToTravelZ3								
							)
						)
	    			);
	            	
	            	solver.add(
	        			ctx.mkGe(
	        				this.scheduledTime(ctx, i, auxFragment), 
							ctx.mkAdd(
									this.arrivalTime(ctx, i, auxFragment),
									this.transmissionTimeZ3								
							)
						)
	    			);
	        	}
	        	/**/
	        	
	            for(int i = 0; i < flowFrag.getNumOfPacketsSent(); i++) {
	                for(int j = 0; j < auxFragment.getNumOfPacketsSent(); j++) {
	                	if (auxExp == null) {
	                        auxExp = ctx.mkFalse();
	                    }
	                	
	                	if(auxFragment == flowFrag && i == j) {
	                        continue;
	                    }
	                    
	                    
	                    /*****************************************************
	                     * 
	                     * Packet A must be transfered after packet B or 
	                     * fit one of the three base cases. 
	                     * 
	                     *****************************************************/
	                    auxExp = ctx.mkOr((BoolExpr) auxExp,
	                            ctx.mkAnd(
	                                ctx.mkAnd(
	                                    ctx.mkEq(auxFragment.getFragmentPriorityZ3(), flowFrag.getFragmentPriorityZ3()),
	                                    ctx.mkLe(this.arrivalTime(ctx, i, flowFrag), this.arrivalTime(ctx, j, auxFragment))
	                                ),
	                                ctx.mkEq(
	                                    this.scheduledTime(ctx, j, auxFragment),
	                                    ctx.mkAdd(
	                                        this.scheduledTime(ctx, i, flowFrag),
	                                        ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3),
	                                        ctx.mkDiv(this.interframeGapSizeZ3, this.portSpeedZ3)
	                                    )
	                                )
	                            )
	                    );
	
	                }
	
	                for(int j = 0; j < this.cycleUpperBoundRange; j++) {
	                    
	                    /*
	                    T2 IS INSIDE SLOT, HAS ENOUGH TIME TO TRANSMIT
	                    ; **************************************
	                    ; |------------------------------------|
	                    ; CS       S    t2-------t3    E       CE
	                    ;               transmission
	                    ; **************************************
	                    */
	                	
	                	for(int index = 0; index < this.cycle.getNumOfSlots(prt); index++) {
	                		indexZ3 = ctx.mkInt(index);
	
	                		/**/
	                		auxExp2 = ctx.mkAnd((BoolExpr) auxExp2, // Arrived during a time slot predicate
	                                ctx.mkImplies( 
	                                    ctx.mkAnd(
	                                        ctx.mkLe(
	                                            this.arrivalTime(ctx, i, flowFrag), 
	                                            ctx.mkSub(
	                                                ctx.mkAdd( 
	                                                    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                    cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                    cycle.cycleStartZ3(ctx, ctx.mkReal(j))
	                                                ), 
	                                                ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
	                                            )
	                                        ),
	                                        ctx.mkGe(
	                                            this.arrivalTime(ctx, i, flowFrag), 
	                                            ctx.mkAdd( 
	                                                cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                cycle.cycleStartZ3(ctx, j)
	                                            )
	                                        )
	                                    ),    
	                                    ctx.mkEq(
	                                        this.scheduledTime(ctx, i, flowFrag),
	                                        ctx.mkAdd(
	                                            this.arrivalTime(ctx, i, flowFrag),
	                                            ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
	                                        )
	                                    )
	                                )
	                        );
	                        /**/
	                        
	                        
	                        /*
	                        ; T2 IS BEFORE THE SLOT
	                        ; **************************************
	                        ; |------------------------------------|
	                        ; CS     t2      S-------t3    E       CE
	                        ;               transmission
	                        ; **************************************
	                        */
	                		
	                		if(index == 0) {
	                            auxExp2 = ctx.mkAnd((BoolExpr) auxExp2, // Arrived before slot start constraint
	                                    ctx.mkImplies( 
	                                        ctx.mkAnd(
	                                            ctx.mkLt(
	                                                this.arrivalTime(ctx, i, flowFrag), 
	                                                ctx.mkAdd(
	                                                    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3), 
	                                                    cycle.cycleStartZ3(ctx, j)
	                                                )
	                                            ),
	                                            ctx.mkGe(
	                                                this.arrivalTime(ctx, i, flowFrag),
	                                                cycle.cycleStartZ3(ctx, j)
	                                            )
	                                        ),
	                                        ctx.mkEq( 
	                                            this.scheduledTime(ctx, i, flowFrag),
	                                            ctx.mkAdd( 
	                                                ctx.mkAdd(
	                                                    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                    cycle.cycleStartZ3(ctx, j)
	                                                ),
	                                                ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
	                                            )
	                                            
	                                        )
	                                    )
	                                ); 
	                		} else if (index < this.cycle.getNumOfSlots(prt)) {
	                			auxExp2 = ctx.mkAnd((BoolExpr) auxExp2,
	                                    ctx.mkImplies( 
	                                        ctx.mkAnd(
	                                            ctx.mkLt(
	                                                this.arrivalTime(ctx, i, flowFrag), 
	                                                ctx.mkAdd(
	                                                    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3), 
	                                                    cycle.cycleStartZ3(ctx, j)
	                                                )
	                                            ),
	                                            ctx.mkGt(
	                                                this.arrivalTime(ctx, i, flowFrag),
	                                                ctx.mkSub(
	                                            		ctx.mkAdd(
	                                                		cycle.cycleStartZ3(ctx, j),
	                                                		cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), ctx.mkInt(index - 1)),
	                                                		cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), ctx.mkInt(index - 1))                                                    
	                                            		),
	                                            		ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
	                                        		)                                                
	                                            )
	                                        ),
	                                        ctx.mkEq( 
	                                            this.scheduledTime(ctx, i, flowFrag),
	                                            ctx.mkAdd( 
	                                                ctx.mkAdd(
	                                                    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                    cycle.cycleStartZ3(ctx, j)
	                                                ),
	                                                ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
	                                            )
	                                            
	                                        )
	                                    )
	                                ); 
	                		}
	                        /**/
	                        
	                		
	                        
	                        /*
	                        ; T2 IS AFTER THE SLOT OR INSIDE WITHOUT ENOUGH TIME. The packet won't be trans-
	                        ; mitted. This happens due to the usage of hyper and micro-cycles.
	                        ; ****************************************************************************
	                        ; |------------------------------------|------------------------------------|
	                        ; CS        S        t2     E        CE/CS       S----------t3   E         CE
	                        ;                                                transmission
	                        ; ****************************************************************************
	                        */
	                		                		
	                		if(index == this.cycle.getNumOfSlots(prt) - 1) {
	                			auxExp2 = ctx.mkAnd((BoolExpr) auxExp2, // Arrived after slot end constraint
	                					ctx.mkImplies( 
	                                        ctx.mkAnd(
	                                            ctx.mkGe(
	                                                this.arrivalTime(ctx, i, flowFrag), 
	                                                cycle.cycleStartZ3(ctx, j)
	                                            ),
	                                            ctx.mkLe(
	                                                this.arrivalTime(ctx, i, flowFrag), 
	                                                ctx.mkAdd(
	                                            		cycle.getCycleDurationZ3(),
	                                                    cycle.cycleStartZ3(ctx, j)		
	                                        		)
	                                            )
	                                        ),
	                                        ctx.mkLe( 
	                                            this.scheduledTime(ctx, i, flowFrag),
	                                            ctx.mkAdd(
	                                      		    cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                                cycle.cycleStartZ3(ctx, j)
	                                            )
	                                        )  
	                                    )         
	                            ); 
	                		}
	                		
	                		/**
	                		* THE CODE BELLOW HAS ISSUES REGARDING NOT COVERING ALL CASES (ALLOWS DELAY).
	                		* REVIEW LATER.
	                		
	                		if(j < this.cycleUpperBoundRange - 1 && index == this.cycle.getNumOfSlots() - 1) {
	                			auxExp2 = ctx.mkAnd((BoolExpr) auxExp2, // Arrived after slot end constraint
	                                    ctx.mkImplies( 
	                                        ctx.mkAnd(
	                                            ctx.mkGt(
	                                                this.arrivalTime(ctx, i, flowFrag), 
	                                                ctx.mkSub(
	                                                    ctx.mkAdd(
	                                                        cycle.slotStartZ3(ctx, flowFrag.getFlowPriority(), indexZ3),
	                                                        cycle.slotDurationZ3(ctx, flowFrag.getFlowPriority(), indexZ3),
	                                                        cycle.cycleStartZ3(ctx, j)
	                                                    ),
	                                                    this.transmissionTimeZ3
	                                                )
	                                            ),
	                                            ctx.mkLe(
	                                                this.arrivalTime(ctx, i, flowFrag),
	                                                ctx.mkAdd(  
	                                                    cycle.cycleStartZ3(ctx, j),
	                                                    cycle.getCycleDurationZ3()
	                                                )
	                                            )
	                                        ),
	                                    
	                                        ctx.mkEq( 
	                                            this.scheduledTime(ctx, i, flowFrag),
	                                            ctx.mkAdd( 
	                                                ctx.mkAdd(
	                                                    cycle.slotStartZ3(ctx, flowFrag.getFlowPriority(), ctx.mkInt(0)),
	                                                    cycle.cycleStartZ3(ctx, j + 1)
	                                                ),
	                                                this.transmissionTimeZ3
	                                            )
	                                        )
	                                    )               
	                            ); 
	                		} else if (j == this.cycleUpperBoundRange - 1 && index == this.cycle.getNumOfSlots() - 1) {
	                			auxExp2 = ctx.mkAnd((BoolExpr) auxExp2,
	                                    ctx.mkImplies( 
	                                        ctx.mkAnd(
	                                            ctx.mkGt(
	                                                this.arrivalTime(ctx, i, flowFrag), 
	                                                ctx.mkSub(
	                                                    ctx.mkAdd(
	                                                        cycle.slotStartZ3(ctx, flowFrag.getFlowPriority(), indexZ3),
	                                                        cycle.slotDurationZ3(ctx, flowFrag.getFlowPriority(), indexZ3),
	                                                        cycle.cycleStartZ3(ctx, j)
	                                                    ),
	                                                    this.transmissionTimeZ3
	                                                )
	                                            ),
	                                            ctx.mkLe(
	                                                this.arrivalTime(ctx, i, flowFrag),
	                                                ctx.mkAdd(  
	                                                    cycle.cycleStartZ3(ctx, j),
	                                                    cycle.getCycleDurationZ3()
	                                                )
	                                            )
	                                        ),
	                                    
	                                        ctx.mkEq( 
	                                            this.scheduledTime(ctx, i, flowFrag),
	                                            this.arrivalTime(ctx, i, flowFrag)
	                                        )
	                                    
	                                    )               
	                            ); 
	                		}     
	                		/**/           		
	                	}
	                }
	
	                //auxExp = ctx.mkOr((BoolExpr)ctx.mkFalse(), (BoolExpr)auxExp2);
	                auxExp = ctx.mkOr((BoolExpr)auxExp, (BoolExpr)auxExp2);
	                
	                
	                if(exp == null) {
	                    exp = auxExp;
	                } else {
	                    exp = ctx.mkAnd((BoolExpr) exp, (BoolExpr) auxExp);
	                }
	                auxExp = ctx.mkFalse();
	                auxExp2 = ctx.mkTrue();
	            }
	        }
	        
	        solver.add(
	        		ctx.mkImplies(
	        				ctx.mkEq(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(prt)), 
	        				(BoolExpr)exp
        			)
    		);
        }
        
        
        auxExp = null;
        exp = ctx.mkFalse(); 
        
        //Every packet must be transmitted inside a timeslot (transmit inside a time slot constraint)
        for(int prt = 0; prt<this.cycle.getNumOfPrts(); prt++) {
	        for(int i = 0; i < flowFrag.getNumOfPacketsSent(); i++) {
	            for(int j = 0; j < this.cycleUpperBoundRange; j++) {
	            	for(int index = 0; index < this.cycle.getNumOfSlots(prt); index++) {
	            		indexZ3 = ctx.mkInt(index);
	                    auxExp = ctx.mkAnd(
	                             ctx.mkGe(
		                            this.scheduledTime(ctx, i, flowFrag), 
		                            ctx.mkAdd(
		                                cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
		                                cycle.cycleStartZ3(ctx, j),
		                                ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
		                            )      
	  	                        ),
		                        ctx.mkLe(
		                            this.scheduledTime(ctx, i, flowFrag), 
		                            ctx.mkAdd(
		                                cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
		                                cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
		                                cycle.cycleStartZ3(ctx, j)
		                            )     
		                        )                        
	                    ); 
		                    
	                    exp = ctx.mkOr((BoolExpr) exp, (BoolExpr) auxExp);
	            	}
	            }
	            solver.add(
	        		ctx.mkImplies(
        				ctx.mkEq(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(prt)), 
        				(BoolExpr) exp
    				)
				);
	            exp = ctx.mkFalse();
	        }
        }
        
        
        /**/
        for(int i = 0; i < flowFrag.getNumOfPacketsSent() - 1; i++) {
            solver.add(
                ctx.mkGe(
                    this.scheduledTime(ctx, i + 1, flowFrag), 
                    ctx.mkAdd(
                            this.scheduledTime(ctx, i, flowFrag),
                            ctx.mkDiv(flowFrag.getPacketSizeZ3(), this.portSpeedZ3)
                    )
                )
            );
        }
        /**/

        Expr wtExp = null;
        if(this.cycle.getWrapTransmission()){

            for(int prt = 0; prt<this.cycle.getNumOfPrts(); prt++) {
	            //A packet either ends at the start of another packet
	            for(int i = 0; i < flowFrag.getNumOfPacketsSent(); i++) {
	                for (FlowFragment auxFlowFrag : this.flowFragments) {
	                    for (int j = 0; j < auxFlowFrag.getNumOfPacketsSent(); j++) {
	
	                        if(flowFrag.name.equals(auxFlowFrag.name) && i!=j){
	                            continue;
	                        }
	
	                        if(wtExp == null){
	                            wtExp = ctx.mkImplies(
	                                    ctx.mkEq(auxFlowFrag.getFragmentPriorityZ3(), flowFrag.getFragmentPriorityZ3()),
	                                    ctx.mkEq(
	                                        this.arrivalTime(ctx, j, auxFlowFrag),
	                                        this.scheduledTime(ctx, i, flowFrag)
	                                    )
	                            );
	                        } else {
	                            wtExp = ctx.mkOr((BoolExpr) wtExp,
	                                    ctx.mkImplies(
	                                        ctx.mkEq(auxFlowFrag.getFragmentPriorityZ3(), flowFrag.getFragmentPriorityZ3()),
	                                        ctx.mkEq(
	                                            this.arrivalTime(ctx, j, auxFlowFrag),
	                                            this.scheduledTime(ctx, i, flowFrag)
	                                        )
	                                    )
	                            );
	                        }
	                    }
	                }
	
	                //Or ends at the end of a cycle
	                for(int j = 0; j < this.cycleUpperBoundRange; j++) {
	                    for(int index = 0; index < this.cycle.getNumOfSlots(prt); index++) {
	                        indexZ3 = ctx.mkInt(index);
	                        wtExp = ctx.mkOr((BoolExpr) wtExp,
	                                ctx.mkEq(
	                                    this.scheduledTime(ctx, i, flowFrag),
	                                    ctx.mkAdd(
	                                        cycle.slotStartZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                        cycle.slotDurationZ3(ctx, flowFrag.getFragmentPriorityZ3(), indexZ3),
	                                        cycle.cycleStartZ3(ctx, j)
	                                    )
	                                )
	                        );
	                    }
	                }
	            }
	            if(wtExp != null){
		            solver.add(
		        		ctx.mkImplies(
	        				ctx.mkEq(flowFrag.getFragmentPriorityZ3(), ctx.mkInt(prt)), 
	        				(BoolExpr) wtExp
	    				)
					);
	            }
        	}
        }


        for(int i = 0; i < flowFrag.getNumOfPacketsSent(); i++) {
            for(FlowFragment auxFlowFrag : this.flowFragments) {
                for(int j = 0; j < auxFlowFrag.getNumOfPacketsSent(); j++) {
                   
                   if(auxFlowFrag.equals(flowFrag)) {
                       continue;
                   }
                    
                   
                    
                   /*
                    * Given that packets from two different flows have
                    * the same priority in this switch, they must not 
                    * be transmitted at the same time
                    * 
                    * OBS: This constraint might not be needed due to the
                    * FIFO constraint.
                    *
                   solver.add(
                       ctx.mkImplies(
                           ctx.mkEq(
                               auxFlowFrag.getFlowPriority(),
                               flowFrag.getFlowPriority()
                           ), 
                           ctx.mkOr(
                               ctx.mkLe(
                                   this.scheduledTime(ctx, i, flowFrag), 
                                   ctx.mkSub(
                                       this.scheduledTime(ctx, j, auxFlowFrag),
                                       this.transmissionTimeZ3
                                   )
                               ),
                               ctx.mkGe(
                                   this.scheduledTime(ctx, i, flowFrag), 
                                   ctx.mkAdd(
                                       this.scheduledTime(ctx, j, auxFlowFrag),
                                       this.transmissionTimeZ3
                                   )
                               )                               
                           )     
                       )
                   );
                   
                   /*
                    * Frame isolation constraint as specified by:
                    *   Craciunas, Silviu S., et al. "Scheduling real-time communication in IEEE 
                    *   802.1 Qbv time sensitive networks." Proceedings of the 24th International 
                    *   Conference on Real-Time Networks and Systems. ACM, 2016. 
                    * 
                    * Two packets from different flows cannot be in the same priority queue 
                    * at the same time 
                    *
                    
                   solver.add(
                       ctx.mkImplies(
                          ctx.mkEq(
                              flowFrag.getFlowPriority(),
                               auxFlowFrag.getFlowPriority()
                           ),  
                           ctx.mkOr(
                               ctx.mkLt(
                                   this.scheduledTime(ctx, i, flowFrag),
                                   this.arrivalTime(ctx, j, auxFlowFrag)
                               ),
                               ctx.mkGt(
                                   this.arrivalTime(ctx, i, flowFrag), 
                                   this.scheduledTime(ctx, j, auxFlowFrag)
                               )
                           )
                       )
                   );
                   /**/

                }
            }
        }
        
        /*
         * If two packets are from the same priority, the first one to arrive
         * should be transmitted first (FIFO priority queue constraint)
         */
        for(int i = 0; i < flowFrag.getNumOfPacketsSent(); i++) {
            for(FlowFragment auxFlowFrag : this.flowFragments) {
                for(int j = 0; j < auxFlowFrag.getNumOfPacketsSent(); j++) {
                    
                	if((flowFrag.equals(auxFlowFrag) && i == j)) {
                		continue;
                	} 
                	
                	solver.add( // Packet transmission order constraint
                        ctx.mkImplies(
                            ctx.mkAnd(
                                ctx.mkLe(
                                    this.arrivalTime(ctx, i, flowFrag),
                                    this.arrivalTime(ctx, j, auxFlowFrag)
                                ),
                                ctx.mkEq(
                                    flowFrag.getFragmentPriorityZ3(), 
                                    auxFlowFrag.getFragmentPriorityZ3()
                                )       
                            ),
                            ctx.mkLe(
                                this.scheduledTime(ctx, i, flowFrag),
                                ctx.mkSub(
                                    this.scheduledTime(ctx, j, auxFlowFrag),
                                    ctx.mkDiv(auxFlowFrag.getPacketSizeZ3(), this.portSpeedZ3),
                                    ctx.mkDiv(this.interframeGapSizeZ3, this.portSpeedZ3)
                                )
            				)
                        )
                    );
                    
                    /*
                    if(!(flowFrag.equals(auxFlowFrag) && i == j)) {
                    	solver.add(	 
                    		ctx.mkNot(
                				ctx.mkEq(
            						this.arrivalTime(ctx, i, flowFrag), 
            						this.arrivalTime(ctx, j, auxFlowFrag)
        						)
            				)
                		);    
                    }
                    /**/
                    
                }
            }
        }
        
    }
   
    
    /**
     * [Method]: setupBestEffort
     * [Usage]: Use in order to enable the best effort traffic reservation
     * constraint.
     * 
     * @param solver	Solver object
     * @param ctx		Context object for the solver
     */
    public void setupBestEffort(Solver solver, Context ctx) {
        RealExpr []slotStart = new RealExpr[8];
        RealExpr []slotDuration = new RealExpr[8];
        // RealExpr guardBandTime = null;        
        
        BoolExpr firstPartOfImplication = null;
        RealExpr sumOfPrtTime = null;
        
        for(int i = 0; i < 8; i++) {
            slotStart[i] = ctx.mkRealConst(this.name + "SlotStart" + i);
            slotDuration[i] = ctx.mkRealConst(this.name + "SlotDuration" + i);
        }
        
        for(int numericFlowPriority = 0; numericFlowPriority < this.cycle.getNumOfPrts(); numericFlowPriority++) {
        	IntExpr flowPriority = ctx.mkInt(numericFlowPriority);
            // RealExpr sumOfSlotsStart = ctx.mkReal(0);
            RealExpr sumOfSlotsDuration = ctx.mkReal(0);
        	
        	for(int i = 0; i < this.cycle.getNumOfSlots(numericFlowPriority); i++) {
        		sumOfSlotsDuration = (RealExpr) ctx.mkAdd(cycle.slotDurationZ3(ctx, flowPriority, ctx.mkInt(i)));
        	}
        	
        	/**/
            for(int i = 0; i < 8; i++) {
                solver.add(
                    ctx.mkImplies(
                        ctx.mkEq(
                        	flowPriority,
                            ctx.mkInt(i)
                        ),
                        ctx.mkEq(
                            slotDuration[i-1],
                            sumOfSlotsDuration
                        )
                        
                    )
                );
            }
            /**/
            
        }
        
        for(int i = 0; i<8; i++) {
            firstPartOfImplication = null;
            
            for(int numericFlowPriority = 0; numericFlowPriority < this.cycle.getNumOfPrts(); numericFlowPriority++) {
            	IntExpr flowPriority = ctx.mkInt(numericFlowPriority);
                if(firstPartOfImplication == null) {
                    firstPartOfImplication = ctx.mkNot(ctx.mkEq(
                    							flowPriority,
                                                ctx.mkInt(i)
                                             ));
                } else {
                    firstPartOfImplication = ctx.mkAnd(firstPartOfImplication, 
                                             ctx.mkNot(ctx.mkEq(
                                        		 flowPriority,
                                                 ctx.mkInt(i)
                                             )));
                } 
            }
            
            solver.add( // Queue not used constraint
                ctx.mkImplies(
                    firstPartOfImplication,
                    ctx.mkAnd(
                        ctx.mkEq(slotStart[i-1], ctx.mkReal(0)),
                        ctx.mkEq(slotDuration[i-1], ctx.mkReal(0))    
                    )
                    
                )                    
            );

        }
        
        for(RealExpr slotDr : slotDuration) {
            if(sumOfPrtTime == null) {
                sumOfPrtTime = slotDr;
            } else {
                sumOfPrtTime = (RealExpr) ctx.mkAdd(sumOfPrtTime, slotDr);
            }
        }
        


        solver.add( // Best-effort bandwidth reservation constraint
            ctx.mkLe(
                sumOfPrtTime,
                ctx.mkMul(
                    ctx.mkSub(
                        ctx.mkReal(1),
                        bestEffortPercentZ3
                    ),                        
                    this.cycle.getCycleDurationZ3()
                )
            )
        );

        /*
        solver.add(
                ctx.mkLe(
                    sumOfPrtTime,
                    ctx.mkMul(bestEffortPercentZ3, this.cycle.getCycleDurationZ3())
                )
            );
       
        solver.add(
            ctx.mkGe(
                bestEffortPercentZ3,
                ctx.mkDiv(sumOfPrtTime, this.cycle.getCycleDurationZ3())
            )
        );
        */
        
    }
    
    /**
     * [Method]: gcd
     * [Usage]: Method used to obtain the greatest common
     * divisor of two values.
     * 
     * @param a		First value
     * @param b		Second value
     * @return		Greatest common divisor or the two previous parameters
     */
    static double gcd(double a, double b) { 
        if (a == 0) {
            return b;             
        }
        
        return gcd(b % a, a); 
    } 
  
    
    /**
     * [Method]: findGCD
     * [Usage]: Retrieves the value of the greatest common divisor 
     * of all the values in an array.
     * 
     * @param arr	Array of double values
     * @return		Greatest common divisor of all values of arr
     */
    static double findGCD(ArrayList<Double> arr) { 
        double gdc = arr.get(0); 
        for (int i = 1; i < arr.size(); i++) {            
            gdc = gcd(arr.get(i), gdc); 
        }
  
        return gdc; 
    } 
    
    
    /**
     * [Method]: findLCM
     * [Usage]: Retrieves the least common multiple of all values in 
     * an array.
     * 
     * @param arr 		Array of double values
     * @return			Least common multiple of all values of arr	
     */
    static double findLCM(ArrayList<Double> arr) {
    	
    	double n = arr.size();
    	
    	double max_num = 0; 
        for (int i = 0; i < n; i++) { 
            if (max_num < arr.get(i)) { 
                max_num = arr.get(i); 
            } 
        } 

        double res = 1; 
 
        double x = 2;  
        while (x <= max_num) {  
            Vector<Integer> indexes = new Vector<>(); 
            for (int j = 0; j < n; j++) { 
                if (arr.get(j) % x == 0) { 
                    indexes.add(indexes.size(), j); 
                } 
            } 
            if (indexes.size() >= 2) { 
                for (int j = 0; j < indexes.size(); j++) { 
                    arr.set(indexes.get(j), arr.get(indexes.get(j)) / x); 
                } 
  
                res = res * x; 
            } else { 
                x++; 
            } 
        } 
  
        for (int i = 0; i < n; i++) { 
            res = res * arr.get(i); 
        } 
  
        return res; 
    }
    
    
    /**
     * [Method]: setUpHyperCycle
     * [Usage]: Set up the cycle duration and number of packets and slots
     * to be scheduled according to the hyper cycle approach.
     * 
     * @param solver	Solver object
     * @param ctx		Context object for the solver
     */
    public void setUpHyperCycle(Solver solver, Context ctx) {
        int numOfPacketsScheduled = 0;

        double hyperCycleSize = findLCM((ArrayList<Double>) listOfPeriods.clone());
        
        this.definedHyperCycleSize = hyperCycleSize;
        
        this.cycleUpperBoundRange = 1;

        /*
        for(FlowFragment flowFrag : this.flowFragments) {
            flowFrag.setNumOfPacketsSent((int) (hyperCycleSize/flowFrag.getStartDevice().getPacketPeriodicity()));
            System.out.println("Frag num packets: " + flowFrag.getNumOfPacketsSent());
            numOfPacketsScheduled += (int) (hyperCycleSize/flowFrag.getStartDevice().getPacketPeriodicity());
        }
        */
        
        for(Double periodicity : this.listOfPeriods) {
        	numOfPacketsScheduled += (int) (hyperCycleSize/periodicity);
        }
        
        // System.out.println("Num of Cycles: " + this.cycleUpperBoundRange);
        
        this.cycle.setNumOfSlots(numOfPacketsScheduled);
        
               
        // In order to use the value cycle time obtained, we must override the minimum and maximum cycle times
        this.cycle.setUpperBoundCycleTime(hyperCycleSize + 1);
        this.cycle.setLowerBoundCycleTime(hyperCycleSize - 1);
        
        
    }
    
    /**
     * [Method]: setUpMicroCycles
     * [Usage]: Set up the cycle duration and number of packets, cycles and slots
     * to be scheduled according to the micro cycle approach.
     * 
     * @param solver	Solver object
     * @param ctx		Context object for the solver
     */
    public void setUpMicroCycles(Solver solver, Context ctx) {
        
        /*       
        for(FlowFragment flowFrag : this.flowFragments) {
            System.out.println(flowFrag.getStartDevice());
            listOfPeriods.add(flowFrag.getStartDevice().getPacketPeriodicity());
        }
        */

        this.microCycleSize = findGCD((ArrayList<Double>) listOfPeriods.clone());
        double hyperCycleSize = findLCM((ArrayList<Double>) listOfPeriods.clone());
        
        this.definedHyperCycleSize = hyperCycleSize;
        
        this.cycleUpperBoundRange = (int) (hyperCycleSize/microCycleSize);

        /*
        for(FlowFragment flowFrag : this.flowFragments) {
            flowFrag.setNumOfPacketsSent((int) (hyperCycleSize/flowFrag.getStartDevice().getPacketPeriodicity()));
            System.out.println("Frag num packets: " + flowFrag.getNumOfPacketsSent());
        }
        System.out.println("Num of Cycles: " + this.cycleUpperBoundRange);
      	*/


        this.cycle.setNumOfSlots(1);
        
                     
        // In order to use the value cycle time obtained, we must override the minimum and maximum cycle times
        this.cycle.setUpperBoundCycleTime(microCycleSize + 1);
        this.cycle.setLowerBoundCycleTime(microCycleSize - 1);
    }
    
    
    /**
     * [Method]: bindTimeSlots
     * [Usage]: IN DEVELOPMENT - Bind timeslots to a fixed name instead
     * of a variable. 
     * 
     * @param solver	Solver object
     * @param ctx		Context object for the solver
     */
    public void bindTimeSlots(Solver solver, Context ctx) {
    	
    	// Ideia = se a prioridade de um flow e' igual a um numero, 
    	// ctx.mkeq nele com o slot the cycle (getSlotS/D(prt, slotnum))
    	for(int prtIndex = 0; prtIndex < this.cycle.getNumOfPrts(); prtIndex++) {
    		for(FlowFragment frag : this.flowFragments) {	
        		for(int slotIndex = 0; slotIndex < this.cycle.getNumOfSlots(prtIndex); slotIndex++) {
            		solver.add(
        				ctx.mkImplies(
        					ctx.mkEq(frag.getFragmentPriorityZ3(), ctx.mkInt(prtIndex)),
        					ctx.mkAnd(
    							ctx.mkEq(
									cycle.slotStartZ3(ctx, frag.getFragmentPriorityZ3(), ctx.mkInt(slotIndex)), 
									cycle.slotStartZ3(ctx, ctx.mkInt(prtIndex), ctx.mkInt(slotIndex)) 
								),
    							ctx.mkEq(
									cycle.slotDurationZ3(ctx, frag.getFragmentPriorityZ3(), ctx.mkInt(slotIndex)), 
									cycle.slotDurationZ3(ctx, ctx.mkInt(prtIndex), ctx.mkInt(slotIndex)) 
								)
							)
        				)	
        			);
        		}
        	}
    	}
    	
    }
    
    /**
     * [Method]: setUpCycle
     * [Usage]: If the port is configured to use a specific automated
     * application period methodology, it will configure its cycle size.
     * Also calls the toZ3 method for the cycle
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     */
    public void setUpCycle(Solver solver, Context ctx) {

    	if(this.cycle.getFirstCycleStartZ3() == null) {
        	this.cycle.toZ3(ctx);
        }

    	// System.out.println("On port: " + this.name + " with: " + this.listOfPeriods.size() + " fragments");

    	if(this.listOfPeriods.size() < 1) {
    		return;
    	}
    	
    	
    	if(useMicroCycles && this.listOfPeriods.size() > 0) {
            setUpMicroCycles(solver, ctx);
                        
            solver.add(
	            ctx.mkEq(this.cycle.getCycleDurationZ3(), ctx.mkReal(Double.toString(this.microCycleSize)))
	        );
            this.cycle.setCycleDuration(this.microCycleSize);
        } else if (useHyperCycle && this.listOfPeriods.size() > 0) {
        	setUpHyperCycle(solver, ctx);

        	solver.add(
	            ctx.mkEq(this.cycle.getCycleDurationZ3(), ctx.mkReal(Double.toString(this.definedHyperCycleSize)))
	        );
            this.cycle.setCycleDuration(this.definedHyperCycleSize);

        }

		if(this.cycle.getCycleStart() > -1){
			solver.add(
					ctx.mkEq( // Equal cycle constraints
							ctx.mkReal(Double.toString(this.cycle.getCycleStart())),
							this.getCycle().getFirstCycleStartZ3()
					)
			);
		}

    }
    
    /**
     * [Method]: zeroOutNonUsedSlots
     * [Usage]: Iterates over the slots adding a constraint that states that
     * if no packet its transmitted inside it, its size must be 0. Can be used
     * to filter out non-used slots and avoid losing bandwidth.
     * 
     * @param solver
     * @param ctx
     */
    public void zeroOutNonUsedSlots(Solver solver, Context ctx) {
    	
    	if(this.useMicroCycles) {
    		return;
    	}
    	
    	BoolExpr exp1;
    	BoolExpr exp2;
    	IntExpr indexZ3;
    	
    	
    	/* BINDTIMESLOTS CONSTRAINT
    	for(int prtIndex = 0; prtIndex < this.cycle.getNumOfPrts(); prtIndex++) {
    		for(FlowFragment frag : this.flowFragments) {	
        		for(int slotIndex = 0; slotIndex < this.cycle.getNumOfSlots(prtIndex); slotIndex++) {
            		solver.add(
        				ctx.mkImplies(
        					ctx.mkEq(frag.getFragmentPriorityZ3(), ctx.mkInt(prtIndex)),
        					ctx.mkAnd(
    							ctx.mkEq(
									cycle.slotStartZ3(ctx, frag.getFragmentPriorityZ3(), ctx.mkInt(slotIndex)), 
									cycle.slotStartZ3(ctx, ctx.mkInt(prtIndex), ctx.mkInt(slotIndex)) 
								),
    							ctx.mkEq(
									cycle.slotDurationZ3(ctx, frag.getFragmentPriorityZ3(), ctx.mkInt(slotIndex)), 
									cycle.slotDurationZ3(ctx, ctx.mkInt(prtIndex), ctx.mkInt(slotIndex)) 
								)
							)
        				)	
        			);
        		}
        	}
    	}
    	*/
    	
		for(int prtIndex = 0; prtIndex < this.cycle.getNumOfPrts(); prtIndex++) {
			for(int cycleNum = 0; cycleNum < this.cycleUpperBoundRange; cycleNum++) {
				for(int indexNum = 0; indexNum < this.cycle.getNumOfSlots(prtIndex); indexNum++) {
					indexZ3 = ctx.mkInt(indexNum);
    				exp1 = ctx.mkTrue();
        			for(FlowFragment frag : this.flowFragments) {
        				for(int packetNum = 0; packetNum < frag.getNumOfPacketsSent(); packetNum++) {
        					exp1 = ctx.mkAnd(
        							exp1,
        							//ctx.mkAnd(
    									ctx.mkNot(
	        								ctx.mkAnd(
	    		    							ctx.mkGe(
    		    									ctx.mkSub(
		    											this.scheduledTime(ctx, packetNum, frag),
	    												ctx.mkDiv(frag.getPacketSizeZ3(), this.portSpeedZ3)
	    											),
	    											ctx.mkAdd( 
	    		                                        cycle.slotStartZ3(ctx, ctx.mkInt(prtIndex), indexZ3),
	    		                                        cycle.cycleStartZ3(ctx, ctx.mkReal(cycleNum))
	    		                                    )
	    										),
	    		    							ctx.mkLe(
	    											this.scheduledTime(ctx, packetNum, frag),
	    											ctx.mkAdd( 
	    		                                        cycle.slotStartZ3(ctx, ctx.mkInt(prtIndex), indexZ3),
	    		                                        cycle.slotDurationZ3(ctx, ctx.mkInt(prtIndex), indexZ3),
	    		                                        cycle.cycleStartZ3(ctx, ctx.mkReal(cycleNum))
	    		                                    )
	    										)
	    									)	
	    								)
    									//,ctx.mkEq(ctx.mkInt(prtIndex), frag.getFragmentPriorityZ3())
									//)
        					);
        				}    	

        			}
    			
    				solver.add(
    					ctx.mkImplies(
							exp1, 	
							ctx.mkEq(cycle.slotDurationZ3(ctx, ctx.mkInt(prtIndex), indexZ3), ctx.mkReal(0)) 
						)
    				);
    			}   
    			
    		}
    		
    	}
    	
    	
    	
    }
    
    
    /**
     * [Method]: setupSchedulingRules
     * [Usage]: Calls the set of functions that will set the z3 rules
     * regarding the cycles, time slots, priorities and timing of packets.
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     */
    public void setupSchedulingRules(Solver solver, Context ctx) {

    	

    	/**/
    	if (this.flowFragments.size() == 0) {
    		solver.add(ctx.mkEq( 
                ctx.mkReal(Double.toString(0)), 
                this.cycle.getCycleDurationZ3()
            ));
    		// solver.add(ctx.mkEq( 
            //    ctx.mkReal(Double.toString(0)), 
            //    this.cycle.getFirstCycleStartZ3()
            // ));
    		
    		return;
    	}
    	/**/
    	
    	
        /*
    	if(useMicroCycles && this.flowFragments.size() > 0) {
    		solver.add(ctx.mkEq( 
                ctx.mkReal(Double.toString(this.microCycleSize)), 
                this.cycle.getCycleDurationZ3()
            ));
        } else if (useHyperCycle && this.flowFragments.size() > 0) {
        	solver.add(ctx.mkEq( 
                ctx.mkReal(Double.toString(this.definedHyperCycleSize)), 
                this.cycle.getCycleDurationZ3()
            ));
        } else {
            for(FlowFragment flowFrag : this.flowFragments) {
                flowFrag.setNumOfPacketsSent(this.packetUpperBoundRange);
            }
        }
    	/**/
        
    	
        setUpCycleRules(solver, ctx);

    	bindTimeSlots(solver,ctx);
        zeroOutNonUsedSlots(solver, ctx);
        
        /*
         * Differently from setUpCycleRules, setupTimeSlots and setupDevPacketTimes
         * need to be called multiple times since there will be a completely new set
         * of rules for each flow fragment on this port.
         */
        
        
        for(FlowFragment flowFrag : this.flowFragments) {
            setupTimeSlots(solver, ctx, flowFrag);
            setupDevPacketTimes(solver, ctx, flowFrag);            
        }
        
        /*
        if(flowFragments.size() > 0) {
            setupBestEffort(solver, ctx);
        }
        /**/
        
    }

    /**
     * [Method]: departureTime
     * [Usage]: Retrieves the departure time of a packet from a flow fragment
     * specified by the index given as a parameter. The departure time is the 
     * time when a packet leaves its previous node with this switch as a destination. 
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param index         Index of the packet of the flow fragment as a z3 variable
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    
    public RealExpr departureTime(Context ctx, IntExpr index, FlowFragment flowFrag){
                
        // If the index is 0, then its the first departure time, else add index * periodicity
        return this.departureTime(ctx, (Integer.parseInt(index.toString())), flowFrag);
        
        /*
        return (RealExpr) ctx.mkITE( 
               ctx.mkGe(index, ctx.mkInt(1)), 
               ctx.mkAdd(
                       flowFrag.getDepartureTimeZ3(Integer.parseInt(index.toString())),
                       ctx.mkMul(flowFrag.getPacketPeriodicity(), index)
                       ), 
               flowFrag.getDepartureTimeZ3(Integer.parseInt(index.toString())));
        /**/
    }
   
    /**
     * [Method]: departureTime
     * [Usage]: Retrieves the departure time of a packet from a flow fragment
     * specified by the index given as a parameter. The departure time is the 
     * time when a packet leaves its previous node with this switch as a destination. 
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex         Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    public RealExpr departureTime(Context ctx, int auxIndex, FlowFragment flowFrag){
    	IntExpr index = null;
    	RealExpr departureTime;
    	int cycleNum = 0;
        
        if(auxIndex + 1 > flowFrag.getNumOfPacketsSent()) {

			cycleNum = (auxIndex - (auxIndex % flowFrag.getNumOfPacketsSent()))/flowFrag.getNumOfPacketsSent();

        	auxIndex = (auxIndex % flowFrag.getNumOfPacketsSent());

        	departureTime = (RealExpr)
        			ctx.mkAdd(
    					flowFrag.getDepartureTimeZ3(auxIndex), 
    					ctx.mkMul(
                            ctx.mkReal(cycleNum),
                            ctx.mkMul(
                                this.cycle.getCycleDurationZ3(),
                                ctx.mkReal(this.cycleUpperBoundRange)
                            )
                        )
					);

        	return departureTime;
        }
        
        departureTime = flowFrag.getDepartureTimeZ3(auxIndex);
        
        return departureTime;
        
    }
    
    
    /**
     * [Method]: arrivalTime
     * [Usage]: Retrieves the arrival time of a packet from a flow fragment
     * specified by the index given as a parameter. The arrival time is the 
     * time when a packet reaches this switch's port.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex      Index of the packet of the flow fragment as a z3 variable
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     *
    public RealExpr arrivalTime(Context ctx, IntExpr index, FlowFragment flowFrag){
       
       
       // The arrival time of this index from the given flow fragment is 
       // equal to the its departure time + time to travel
        
       return (RealExpr) ctx.mkAdd(
                      departureTime(ctx, index, flowFrag),
                      timeToTravelZ3
                      );
    }
    /**/
    
    /**
     * [Method]: arrivalTime
     * [Usage]: Retrieves the arrival time of a packet from a flow fragment
     * specified by the index given as a parameter. The arrival time is the 
     * time when a packet reaches this switch's port.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex      Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    public RealExpr arrivalTime(Context ctx, int auxIndex, FlowFragment flowFrag){
        IntExpr index = ctx.mkInt(auxIndex);
        
        return (RealExpr) ctx.mkAdd( // Arrival time value constraint
                        departureTime(ctx, index, flowFrag),
                        timeToTravelZ3
                        );
    }
   
    /**
     * [Method]: scheduledTime
     * [Usage]: Retrieves the scheduled time of a packet from a flow fragment
     * specified by the index given as a parameter. The scheduled time is the 
     * time when a packet leaves this switch for its next destination.
     * 
     * Since the scheduled time is an unknown value, it won't be specified as an
     * if and else situation or an equation like the departure or arrival time.
     * Instead, given a flow fragment and an index, this function will return the
     * name of the z3 variable that will be the queried to the solver.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param index         Index of the packet of the flow fragment as a z3 variable
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the scheduled time of the desired packet
     *
    public RealExpr scheduledTime(Context ctx, IntExpr index, FlowFragment flowFrag){
        RealExpr devT3 = ctx.mkRealConst(flowFrag.getName() + "ScheduledTime" + index.toString());
        
        return (RealExpr) devT3;
    }
    /**/
    
    /**
     * [Method]: scheduledTime
     * [Usage]: Retrieves the scheduled time of a packet from a flow fragment
     * specified by the index given as a parameter. The scheduled time is the 
     * time when a packet leaves this switch for its next destination.
     * 
     * Since the scheduled time is an unknown value, it won't be specified as an
     * if and else situation or an equation like the departure or arrival time.
     * Instead, given a flow fragment and an index, this function will return the
     * name of the z3 variable that will be the queried to the solver.
     * 
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     * @param auxIndex         Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the scheduled time of the desired packet
     */
    public RealExpr scheduledTime(Context ctx, int auxIndex, FlowFragment flowFrag){
    	IntExpr index = null;
    	RealExpr scheduledTime;
    	int cycleNum = 0;
    	
        if(auxIndex + 1 > flowFrag.getNumOfPacketsSent()) {
			cycleNum = (auxIndex - (auxIndex % flowFrag.getNumOfPacketsSent()))/flowFrag.getNumOfPacketsSent();

        	auxIndex = (auxIndex % flowFrag.getNumOfPacketsSent());
        	index = ctx.mkInt(auxIndex);
        	
        	scheduledTime = (RealExpr)
        			ctx.mkAdd(
    					ctx.mkRealConst(flowFrag.getName() + "ScheduledTime" + index.toString()), 
    					ctx.mkMul(
                            ctx.mkReal(cycleNum),
                            ctx.mkMul(
                                this.cycle.getCycleDurationZ3(),
                                ctx.mkReal(this.cycleUpperBoundRange)
                            )
                        )
					);
        	
        	
        	return scheduledTime;
        }
        
        index = ctx.mkInt(auxIndex);
        
        
        scheduledTime = ctx.mkRealConst(flowFrag.getName() + "ScheduledTime" + index.toString());
        
        return (RealExpr) scheduledTime;
    }

    
    /**
     * [Method]: checkIfAutomatedApplicationPeriod
     * [Usage]: Returns true if the port uses an automated application period
     * methodology.
     * 
     * @return boolean value. True if automated application period methodology is used, false elsewhise
     */
    public Boolean checkIfAutomatedApplicationPeriod() {
    	if(this.useHyperCycle || this.useMicroCycles)
    		return true;
    	return false;
    }
    
    
    /**
     * [Method]: loadNetwork
     * [Usage]: From the primitive values retrieved in the object
     * deserialization process, instantiate the z3 objects that represent
     * the same properties.
     * 
     * @param ctx		Context object for the solver
     * @param solver	Solver object
     */
    public void loadZ3(Context ctx, Solver solver) {
    	
//    	System.out.println("Loading port " + this.name + " with duration " + this.cycle.getCycleDurationZ3());
    	
		this.cycle.loadZ3(ctx, solver);
    	
    	for(FlowFragment frag : this.flowFragments) {
    		
    		frag.setFragmentPriorityZ3(
				ctx.mkInt(
					frag.getFragmentPriority()				
				)
			);
    		
    		/*
    		solver.add(
				ctx.mkEq(
					frag.getFragmentPriorityZ3(),
					ctx.mkInt(frag.getFragmentPriority())					
				)
			);
    		*/
    		
    		for(int index = 0; index < this.cycle.getNumOfSlots(frag.getFragmentPriority()); index++) {
    			solver.add(
					ctx.mkEq(
						this.cycle.slotDurationZ3(ctx, frag.getFragmentPriorityZ3(), ctx.mkInt(index)), 
						ctx.mkReal(
							Double.toString(
								this.cycle.getSlotDuration(frag.getFragmentPriority(), index)
							)
						)
					)
				);
    			
    			solver.add(
					ctx.mkEq(
						this.cycle.slotStartZ3(ctx, frag.getFragmentPriorityZ3(), ctx.mkInt(index)), 
						ctx.mkReal(
							Double.toString(
								this.cycle.getSlotStart(frag.getFragmentPriority(), index)
							)
						)
					)
				);
    			
    		}
    		
    		for(int i = 0; i < frag.getNumOfPacketsSent(); i++) {
    			/*
    			solver.add(
					ctx.mkEq(
						this.departureTime(ctx, i, frag),
						ctx.mkReal(Double.toString(frag.getDepartureTime(i)))
					)
				);
    			*/
    			if (i > 0)
    				frag.addDepartureTimeZ3(ctx.mkReal(Double.toString(frag.getDepartureTime(i))));
    			
    			solver.add(
					ctx.mkEq(
						this.arrivalTime(ctx, i, frag),
						ctx.mkReal(Double.toString(frag.getArrivalTime(i)))
					)
				);
    			
    			solver.add(
					ctx.mkEq(
						this.scheduledTime(ctx, i, frag),
						ctx.mkReal(Double.toString(frag.getScheduledTime(i)))
					)
				);
    			
    		}
    		
    	}
    	
    	
    }
    
    /*
     * GETTERS AND SETTERS:
     */
    
    public void clearScheduleType() {
    	this.useHyperCycle = false;
    	this.useMicroCycles = false;
    }

    public Cycle getCycle() {
        return cycle;
    }

    public void setCycle(Cycle cycle) {
        this.cycle = cycle;
    }

    public ArrayList<FlowFragment> getDeviceList() {
        return flowFragments;
    }

    public void setDeviceList(ArrayList<FlowFragment> flowFragments) {
        this.flowFragments = flowFragments;
    }
    
    public void addToFragmentList(FlowFragment flowFrag) {
        this.flowFragments.add(flowFrag);
    }
    
    public double getGbSize() {
        return gbSize;
    }

    public void setGbSize(double gbSize) {
        this.gbSize = gbSize;
    }

    public RealExpr getGbSizeZ3() {
        return gbSizeZ3;
    }

    public void setGbSizeZ3(RealExpr gbSizeZ3) {
        this.gbSizeZ3 = gbSizeZ3;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectsTo() {
        return connectsTo;
    }

    public void setConnectsTo(String connectsTo) {
        this.connectsTo = connectsTo;
    }
    
    public void addToListOfPeriods(Double period) {
    	this.listOfPeriods.add(period);
    }
    
    public ArrayList<Double> getListOfPeriods() {
		return listOfPeriods;
	}

	public void setListOfPeriods(ArrayList<Double> listOfPeriods) {
		this.listOfPeriods = listOfPeriods;
	}

    public int getCycleUpperBoundRange() {
		return cycleUpperBoundRange;
	}

	public void setCycleUpperBoundRange(int cycleUpperBoundRange) {
		this.cycleUpperBoundRange = cycleUpperBoundRange;
	}

	public double getDefinedHyperCycleSize() {
		return definedHyperCycleSize;
	}

	public void setDefinedHyperCycleSize(double definedHyperCycleSize) {
		this.definedHyperCycleSize = definedHyperCycleSize;
	}    
	
	public int getPortNum() {
		return portNum;
	}

	public void setPortNum(int portNum) {
		this.portNum = portNum;
	}

    public ArrayList<FlowFragment> getFlowFragments() {
        return flowFragments;
    }

    public void setFlowFragments(ArrayList<FlowFragment> flowFragments) {
        this.flowFragments = flowFragments;
    }
    
    
    /***************************************************
     * 
     * The methods bellow are not completely operational
     * and are currently not used in the project. Might be
     * useful in future iterations of TSNsched.
     * 
     ***************************************************/
    
    public IntExpr getCycleOfScheduledTime(Context ctx, FlowFragment f, int index) {
        IntExpr cycleIndex = null;
        
        RealExpr relativeST = (RealExpr) ctx.mkSub(
                this.scheduledTime(ctx, index, f),
                this.cycle.getFirstCycleStartZ3()
        );

        cycleIndex = ctx.mkReal2Int(
                        (RealExpr) ctx.mkDiv(relativeST, this.cycle.getCycleDurationZ3())
                     );
             
        return cycleIndex;        
    }
    
    
    public IntExpr getCycleOfTime(Context ctx, RealExpr time) {
        IntExpr cycleIndex = null;
        
        RealExpr relativeST = (RealExpr) ctx.mkSub(
                time,
                this.cycle.getFirstCycleStartZ3()
        );

        cycleIndex = ctx.mkReal2Int(
                        (RealExpr) ctx.mkDiv(relativeST, this.cycle.getCycleDurationZ3())
                     );
             
        return cycleIndex;        
    }
    
    public RealExpr getScheduledTimeOfPreviousPacket(Context ctx, FlowFragment f, int index) {
        RealExpr prevPacketST = ctx.mkReal(0);
        
        for(FlowFragment auxFrag : this.flowFragments) {
            
            for(int i = 0; i < f.getNumOfPacketsSent(); i++) {
                prevPacketST = (RealExpr)
                        ctx.mkITE(
                            ctx.mkAnd(
                                ctx.mkEq(auxFrag.getFragmentPriorityZ3(), f.getFragmentPriorityZ3()),
                                ctx.mkLt(
                                    this.scheduledTime(ctx, i, auxFrag),
                                    this.scheduledTime(ctx, index, f)
                                ),
                                ctx.mkGt(
                                    this.scheduledTime(ctx, i, auxFrag),
                                    prevPacketST
                                )
                            ),
                            this.scheduledTime(ctx, i, auxFrag),
                            prevPacketST
                            
                        );
                
            }
           
        }
        
        return (RealExpr)  
                ctx.mkITE(
                    ctx.mkEq(prevPacketST, ctx.mkReal(0)),
                    this.scheduledTime(ctx, index, f),
                    prevPacketST
                );
    }
    
    public double getPortSpeed() {
		return portSpeed;
	}


	public void setPortSpeed(double portSpeed) {
		this.portSpeed = portSpeed;
	}


	public Boolean getIsModifiedOrCreated() {
		return isModifiedOrCreated;
	}

	public void setIsModifiedOrCreated(Boolean isModifiedOrCreated) {
		this.isModifiedOrCreated = isModifiedOrCreated;
	}


	public Boolean getUseMicroCycles() {
		return useMicroCycles;
	}


    public double getTimeToTravel() {
		return timeToTravel;
	}


	public void setTimeToTravel(double timeToTravel) {
		this.timeToTravel = timeToTravel;
	}



	public void setUseMicroCycles(Boolean useMicroCycles) {
		this.useMicroCycles = useMicroCycles;
	}


	public Boolean getUseHyperCycle() {
		return useHyperCycle;
	}


	public void setUseHyperCycle(Boolean useHyperCycle) {
		this.useHyperCycle = useHyperCycle;
	}


	public void setCycleStart(Double cycleStart) {
		this.cycle.setCycleStart(cycleStart);
		
	}

		
	
}