package schedule_generator;

import java.util.ArrayList;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;

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
public class Port {
    private String name;
    private String connectsTo;
    
    private float bestEffortPercent = 0.5f;
    RealExpr bestEffortPercentZ3;
    
    private Cycle cycle;
    private ArrayList<FlowFragment> flowFragments;
    
    public ArrayList<FlowFragment> getFlowFragments() {
        return flowFragments;
    }


    public void setFlowFragments(ArrayList<FlowFragment> flowFragments) {
        this.flowFragments = flowFragments;
    }

    private int packetUpperBoundRange = Network.PACKETUPPERBOUNDRANGE; // Limits the applications of rules to the packets
    private int cycleUpperBoundRange = Network.CYCLEUPPERBOUNDRANGE; // Limits the applications of rules to the cycles
    private float gbSize;
    
    protected float maxPacketSize;
    protected float timeToTravel;
    protected float transmissionTime;
    protected float portSpeed;
    protected int portNum;
    
    private RealExpr gbSizeZ3; // Size of the guardBand
    protected RealExpr maxPacketSizeZ3;
    protected RealExpr timeToTravelZ3;
    protected RealExpr transmissionTimeZ3;
    protected RealExpr portSpeedZ3;
    
    
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
            String connectsTo,
            float maxPacketSize,
            float timeToTravel,
            float transmissionTime,
            float portSpeed,
            float gbSize,
            Cycle cycle) {
        this.name = name;
        this.connectsTo = connectsTo;
        this.maxPacketSize = maxPacketSize;
        this.timeToTravel = timeToTravel;
        this.transmissionTime = transmissionTime;
        this.portSpeed = portSpeed;
        this.gbSize = gbSize;
        this.cycle = cycle;
        this.flowFragments = new ArrayList<FlowFragment>();
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
        this.gbSizeZ3 = ctx.mkReal(Float.toString(gbSize));
        this.maxPacketSizeZ3 = ctx.mkReal(Float.toString(this.maxPacketSize));
        this.timeToTravelZ3 = ctx.mkReal(Float.toString(this.timeToTravel));
        this.transmissionTimeZ3 = ctx.mkReal(Float.toString(this.transmissionTime));
        this.portSpeedZ3 = ctx.mkReal(Float.toString(portSpeed));
        this.bestEffortPercentZ3 = ctx.mkReal(Float.toString(bestEffortPercent));
        this.cycle.toZ3(ctx);
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

        for(FlowFragment frag : this.flowFragments) {
            IntExpr flowPriority = frag.getFlowPriority();
            
            // A slot will be somewhere between 0 and the end of the cycle minus its duration
            solver.add(ctx.mkGe(cycle.slotStartZ3(ctx, flowPriority), ctx.mkInt(0)));
            solver.add(
                ctx.mkLe(cycle.slotStartZ3(ctx, flowPriority), 
                    ctx.mkSub(
                        cycle.getCycleDurationZ3(),
                        cycle.slotDurationZ3(ctx, flowPriority)
                    )
                )
            );
             
            // Every slot duration is greater or equal 0 and lower or equal than the maximum
            solver.add(ctx.mkGe(cycle.slotDurationZ3(ctx, flowPriority), ctx.mkInt(0)));
            solver.add(ctx.mkLe(cycle.slotDurationZ3(ctx, flowPriority), cycle.getMaximumSlotDurationZ3()));
            
            //Every slot must fit inside a cycle
            solver.add(
                ctx.mkGe(
                    cycle.getCycleDurationZ3(), 
                    ctx.mkAdd(
                        cycle.slotStartZ3(ctx, flowPriority), 
                        cycle.slotDurationZ3(ctx, flowPriority)
                    )
                )
            );
            
            /*
             * If the priority of the fragments are the same, then the start and duration
             * of a slot is also the same (needs to be specified due to z3 variable naming
             * properties)
             */
            
            for (FlowFragment auxFrag : this.flowFragments) {
                solver.add(
                    ctx.mkImplies(
                        ctx.mkEq(frag.getFlowPriority(), auxFrag.getFlowPriority()), 
                        ctx.mkAnd(
                                
                             ctx.mkEq(
                                     cycle.slotStartZ3(ctx, frag.getFlowPriority()),
                                     cycle.slotStartZ3(ctx, auxFrag.getFlowPriority())
                             ),
                             ctx.mkEq(
                                     cycle.slotDurationZ3(ctx, frag.getFlowPriority()),
                                     cycle.slotDurationZ3(ctx, auxFrag.getFlowPriority())
                             )
                             
                        )    
                    )
                );
            }
        
            //The start of a slot cannot be inside of another slot
            for (FlowFragment auxFrag : this.flowFragments) {
                if(auxFrag.equals(frag)) {
                    continue;
                }
                
                IntExpr auxFlowPriority = auxFrag.getFlowPriority();
                
                solver.add(
                    ctx.mkImplies(
                        ctx.mkNot(
                            ctx.mkEq(
                                flowPriority,
                                auxFlowPriority)
                            ),
                            ctx.mkNot(ctx.mkAnd(
                                ctx.mkGe(
                                    cycle.slotStartZ3(ctx, flowPriority), 
                                    cycle.slotStartZ3(ctx, auxFlowPriority)
                                ),
                                ctx.mkLe(
                                    cycle.slotStartZ3(ctx, flowPriority),
                                    ctx.mkAdd(
                                        cycle.slotStartZ3(ctx, auxFlowPriority),
                                        cycle.slotDurationZ3(ctx, auxFlowPriority)
                                    )
                                )                                        
                            )
                        )
                    )
                );
            }
        
            // The end of a slot cannot be inside of another slot
            for (FlowFragment auxFrag : this.flowFragments) {
                if(auxFrag.equals(frag)) {
                    continue;
                }
                
                IntExpr auxFlowPriority = auxFrag.getFlowPriority();
                solver.add(
                    ctx.mkImplies(
                        ctx.mkNot(
                            ctx.mkEq(
                                flowPriority,
                                auxFlowPriority
                            )
                        ),
                        ctx.mkNot(
                            ctx.mkAnd(
                                ctx.mkLe(
                                    cycle.slotStartZ3(ctx, auxFlowPriority),
                                    ctx.mkAdd(
                                        cycle.slotStartZ3(ctx, flowPriority),
                                        cycle.slotDurationZ3(ctx, flowPriority)
                                    )
                                ),
                                ctx.mkLe(
                                    ctx.mkAdd(
                                        cycle.slotStartZ3(ctx, flowPriority),
                                        cycle.slotDurationZ3(ctx, flowPriority)
                                    ),
                                    ctx.mkAdd(
                                        cycle.slotStartZ3(ctx, auxFlowPriority),
                                        cycle.slotDurationZ3(ctx, auxFlowPriority)
                                    )
                                )                                        
                            )
                        )
                    )
                );
            }
            
            
            /*
             * If 2 slots are not consecutive, then there must be a space
             * of at least gbSize (the size of the guard band) between them
             */
            for (FlowFragment auxFrag : this.flowFragments) {
                
                if(auxFrag.equals(frag)) {
                    continue;
                }
                
                IntExpr auxFlowPriority = auxFrag.getFlowPriority();
                
                solver.add(
                    ctx.mkImplies(
                        ctx.mkAnd(
                            ctx.mkNot(
                                ctx.mkEq(
                                    cycle.slotStartZ3(ctx, flowPriority), 
                                    ctx.mkAdd(
                                        cycle.slotDurationZ3(ctx, auxFlowPriority),
                                        cycle.slotStartZ3(ctx, auxFlowPriority)
                                    )                                                
                                )
                            ),
                            ctx.mkGt(
                                cycle.slotStartZ3(ctx, flowPriority), 
                                cycle.slotStartZ3(ctx, auxFlowPriority)
                            )
                        ),
                        ctx.mkOr(
                            ctx.mkLe(
                                ctx.mkAdd(
                                    cycle.slotStartZ3(ctx, flowPriority),
                                    cycle.slotDurationZ3(ctx, flowPriority)
                                ),
                                ctx.mkAdd(
                                    cycle.slotStartZ3(ctx, auxFlowPriority),
                                    gbSizeZ3
                                )
                            ),
                            ctx.mkGe(
                                cycle.slotStartZ3(ctx, flowPriority),
                                ctx.mkAdd(
                                    cycle.slotStartZ3(ctx, auxFlowPriority),
                                    cycle.slotDurationZ3(ctx, auxFlowPriority),
                                    gbSizeZ3
                                )   
                            )
                        )
                    )
                );
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
        // If there is a flow assigned to the slot, slotDuration must be greater than transmission time
        solver.add(ctx.mkGe(cycle.slotDurationZ3(ctx, flowFrag.getFlowPriority()), this.transmissionTimeZ3));
        
        // Every flow must have a priority
        solver.add(ctx.mkGt(flowFrag.getFlowPriority(), ctx.mkInt(0)));
        solver.add(ctx.mkLt(flowFrag.getFlowPriority(), ctx.mkInt(9)));
        
        // Slot start must be <= cycle time - slot duration 
        solver.add(
            ctx.mkLe(
                ctx.mkAdd(
                    cycle.slotDurationZ3(ctx, flowFrag.getFlowPriority()),
                    cycle.slotStartZ3(ctx, flowFrag.getFlowPriority())
                ), 
                cycle.getCycleDurationZ3()
            )
        );

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
        for(int i = 0; i < this.packetUpperBoundRange; i++) {
            // Make t3 > t2 + transmissionTime
            solver.add(
                ctx.mkGe(
                    this.scheduledTime(ctx, i, flowFrag),
                    ctx.mkAdd(this.arrivalTime(ctx, i, flowFrag), this.transmissionTimeZ3)
                )
            );
            
        }
        
        Expr auxExp = null;
        Expr auxExp2 = ctx.mkTrue();
        Expr exp = null;
        
        for(FlowFragment auxFragment : this.flowFragments) {
            for(int i = 0; i < this.packetUpperBoundRange; i++) {
                for(int j = 0; j < this.packetUpperBoundRange; j++) {
                    if(auxFragment == flowFrag && i == j) {
                        continue;
                    }
                    
                    
                    /*****************************************************
                     * 
                     * Packet A must be transfered after packet B or 
                     * fit one of the three base cases.
                     * 
                     *****************************************************/
                    if(auxExp == null) {
                        auxExp = 
                                ctx.mkAnd(
                                    ctx.mkAnd(
                                        ctx.mkEq(auxFragment.getFlowPriority(), flowFrag.getFlowPriority()),
                                        ctx.mkLt(this.arrivalTime(ctx, j, flowFrag), this.arrivalTime(ctx, i, auxFragment))
                                    ),
                                    ctx.mkEq(
                                        this.scheduledTime(ctx, i, auxFragment),
                                        ctx.mkAdd(
                                            this.scheduledTime(ctx, j, flowFrag),
                                            this.transmissionTimeZ3
                                        )
                                    )
                                
                        );
                    } else {
                        auxExp = ctx.mkOr((BoolExpr) auxExp,
                                ctx.mkAnd(
                                    ctx.mkAnd(
                                        ctx.mkEq(auxFragment.getFlowPriority(), flowFrag.getFlowPriority()),
                                        ctx.mkLt(this.arrivalTime(ctx, j, flowFrag), this.arrivalTime(ctx, i, auxFragment))
                                    ),
                                    ctx.mkEq(
                                        this.scheduledTime(ctx, i, auxFragment),
                                        ctx.mkAdd(
                                            this.scheduledTime(ctx, j, flowFrag),
                                            this.transmissionTimeZ3
                                        )
                                    )
                                )
                        );
                    }
                    
                    
                    
                }
                
                /*
                auxExp = ctx.mkOr((BoolExpr) auxExp,
                        ctx.mkEq(
                            this.scheduledTime(ctx, i, auxFragment),
                            ctx.mkAdd(
                                this.arrivalTime(ctx, i, flowFrag),
                                this.transmissionTimeZ3
                            )
                        )
                );
                */
                
                for(int j = 0; j < this.cycleUpperBoundRange; j++) {
                    
                    /*
                    T2 IS INSIDE SLOT, HAS ENOUGH TIME TO TRANSMIT
                    ; **************************************
                    ; |------------------------------------|
                    ; CS       S    t2-------t3    E       CE
                    ;               transmission
                    ; **************************************
                    */
                    auxExp2 = ctx.mkAnd((BoolExpr) auxExp2,
                            ctx.mkImplies( 
                                ctx.mkAnd(
                                    ctx.mkLe(
                                        this.arrivalTime(ctx, i, auxFragment), 
                                        ctx.mkSub(
                                            ctx.mkAdd( 
                                                cycle.slotStartZ3(ctx, auxFragment.getFlowPriority()),
                                                cycle.slotDurationZ3(ctx, auxFragment.getFlowPriority()),
                                                cycle.cycleStartZ3(ctx, ctx.mkInt(j))
                                            ), 
                                            this.transmissionTimeZ3
                                        )
                                    ),
                                    ctx.mkGe(
                                        this.arrivalTime(ctx, i, auxFragment), 
                                        ctx.mkAdd( 
                                            cycle.slotStartZ3(ctx, auxFragment.getFlowPriority()),
                                            cycle.cycleStartZ3(ctx, j)
                                        )
                                    )
                                ),    
                                ctx.mkEq(
                                    this.scheduledTime(ctx, i, auxFragment),
                                    ctx.mkAdd(
                                        this.arrivalTime(ctx, i, auxFragment),
                                        this.transmissionTimeZ3
                                    )
                                )
                            )
                    );
                    /**/
                    
                    /**
                    auxExp = ctx.mkOr((BoolExpr) auxExp,
                            ctx.mkEq(
                                this.scheduledTime(ctx, i, auxFragment),
                                ctx.mkAdd( 
                                    ctx.mkAdd(
                                        cycle.slotStartZ3(ctx, flowFrag.getFlowPriority()),
                                        cycle.cycleStartZ3(ctx, j)
                                    ),
                                    this.transmissionTimeZ3
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
                    auxExp2 = ctx.mkAnd((BoolExpr) auxExp2,
                            ctx.mkImplies( 
                                ctx.mkAnd(
                                    ctx.mkLt(
                                        this.arrivalTime(ctx, i, auxFragment), 
                                        ctx.mkAdd(
                                            cycle.slotStartZ3(ctx, auxFragment.getFlowPriority()), 
                                            cycle.cycleStartZ3(ctx, j)
                                        )
                                    ),
                                    ctx.mkGe(
                                        this.arrivalTime(ctx, i, auxFragment),
                                        cycle.cycleStartZ3(ctx, j)
                                    )
                                ),
                                ctx.mkEq( 
                                    this.scheduledTime(ctx, i, auxFragment),
                                    ctx.mkAdd( 
                                        ctx.mkAdd(
                                            cycle.slotStartZ3(ctx, auxFragment.getFlowPriority()),
                                            cycle.cycleStartZ3(ctx, j)
                                        ),
                                        this.transmissionTimeZ3
                                    )
                                    
                                )
                            )
                        ); 
                    
                    /*
                    ; T2 IS AFTER THE SLOT OR INSIDE WITHOUT ENOUGH TIME
                    ; ****************************************************************************
                    ; |------------------------------------|------------------------------------|
                    ; CS        S        t2     E        CE/CS       S----------t3   E         CE
                    ;                                                transmission
                    ; ****************************************************************************
                    */
                    auxExp2 = ctx.mkAnd((BoolExpr) auxExp2,
                            ctx.mkImplies( 
                                ctx.mkAnd(
                                    ctx.mkGt(
                                        this.arrivalTime(ctx, i, auxFragment), 
                                        ctx.mkSub(
                                            ctx.mkAdd(
                                                cycle.slotStartZ3(ctx, auxFragment.getFlowPriority()),
                                                cycle.slotDurationZ3(ctx, auxFragment.getFlowPriority()),
                                                cycle.cycleStartZ3(ctx, j)
                                            ),
                                            this.transmissionTimeZ3
                                        )
                                    ),
                                    ctx.mkLe(
                                        this.arrivalTime(ctx, i, auxFragment),
                                        ctx.mkAdd(  
                                            cycle.cycleStartZ3(ctx, j),
                                            cycle.getCycleDurationZ3()
                                        )
                                    )
                                ),
                            
                                ctx.mkEq( 
                                    this.scheduledTime(ctx, i, auxFragment),
                                    ctx.mkAdd( 
                                        ctx.mkAdd(
                                            cycle.slotStartZ3(ctx, auxFragment.getFlowPriority()),
                                            cycle.cycleStartZ3(ctx, j + 1)
                                        ),
                                        this.transmissionTimeZ3
                                    )
                                )
                            
                            )               
                    ); 
                }
                
                auxExp = ctx.mkOr((BoolExpr)auxExp, (BoolExpr)auxExp2);
                
                if(exp == null) {
                    exp = auxExp;
                } else {
                    exp = ctx.mkAnd((BoolExpr) exp, (BoolExpr) auxExp);
                }
            }
        }

        solver.add((BoolExpr)exp);
        
        auxExp = null;
        exp = ctx.mkFalse(); 
        
        for(int i = 0; i < this.packetUpperBoundRange; i++) {
            for(int j = 0; j < this.cycleUpperBoundRange; j++) {
                auxExp = ctx.mkAnd(
                    ctx.mkGe(
                        this.scheduledTime(ctx, i, flowFrag), 
                        ctx.mkAdd(
                            cycle.slotStartZ3(ctx, flowFrag.getFlowPriority()),
                            cycle.cycleStartZ3(ctx, j),
                            this.transmissionTimeZ3
                        )      
                    ),
                    ctx.mkLe(
                        this.scheduledTime(ctx, i, flowFrag), 
                        ctx.mkAdd(
                            cycle.slotStartZ3(ctx, flowFrag.getFlowPriority()),
                            cycle.slotDurationZ3(ctx, flowFrag.getFlowPriority()),
                            cycle.cycleStartZ3(ctx, j)
                        )     
                    )                        
                ); 
                
                exp = ctx.mkOr((BoolExpr) exp, (BoolExpr) auxExp);
            }
            solver.add((BoolExpr) exp);
            exp = ctx.mkFalse();
        }
        
        
        /**/
        for(int i = 0; i < this.packetUpperBoundRange; i++) {
            solver.add(
                ctx.mkGe(
                    this.scheduledTime(ctx, i + 1, flowFrag), 
                    ctx.mkAdd(
                            this.scheduledTime(ctx, i, flowFrag),
                            this.transmissionTimeZ3
                    )
                )
            );
        }
        /**/
        
        
        
        for(int i = 0; i < this.packetUpperBoundRange; i++) {
            for(int j = 0; j < this.packetUpperBoundRange; j++) {
                for(FlowFragment auxFlowFrag : this.flowFragments) {
                   
                   if(auxFlowFrag.equals(flowFrag)) {
                       continue;
                   }
                    
                   
                    
                   /*
                    * Given that packets from two different flows have
                    * the same priority in this switch, they must not 
                    * be transmitted at the same time
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
                    */
                    
//                   solver.add(
//                       ctx.mkImplies(
//                           ctx.mkEq(
//                               flowFrag.getFlowPriority(),
//                               auxFlowFrag.getFlowPriority()
//                           ),  
//                           ctx.mkOr(
//                               ctx.mkLt(
//                                   this.scheduledTime(ctx, i, flowFrag),
//                                   this.arrivalTime(ctx, j, auxFlowFrag)
//                               ),
//                               ctx.mkGt(
//                                   this.arrivalTime(ctx, i, flowFrag), 
//                                   this.scheduledTime(ctx, j, auxFlowFrag)
//                               )
//                           )
//                       )
//                   );

                }
            }
        }
        
        /*
         * If two packets are from the same priority, the first one to arrive
         * should be transmitted first (FIFO)
         */
        for(int i = 0; i < this.packetUpperBoundRange; i++) {
            for(int j = 0; j < this.packetUpperBoundRange; j++) {
                for(FlowFragment auxFlowFrag : this.flowFragments) {
                    
                    if((flowFrag.equals(auxFlowFrag) && i == j)) {
                		continue;
                	} 
                   
                    solver.add(
                        ctx.mkImplies(
                            ctx.mkAnd(
                                ctx.mkLe(
                                    this.arrivalTime(ctx, i, flowFrag),
                                    this.arrivalTime(ctx, j, auxFlowFrag)
                                ),
                                ctx.mkEq(
                                    flowFrag.getFlowPriority(), 
                                    auxFlowFrag.getFlowPriority()
                                )       
                            ),
                            ctx.mkLe(
                                this.scheduledTime(ctx, i, flowFrag),
                                ctx.mkSub(
                                        this.scheduledTime(ctx, j, auxFlowFrag),
                                        this.transmissionTimeZ3
                                )
                            )
                        )
                    );
                    
                }
            }
        }
        
    }
    

    public void setupBestEffort(Solver solver, Context ctx) {
        RealExpr counter = null;
        RealExpr currentTime = null;
        RealExpr totalTime = null;
        

        RealExpr []slotStart = new RealExpr[8];
        RealExpr []slotDuration = new RealExpr[8];
        RealExpr guardBandTime = null;        
        
        BoolExpr firstPartOfImplication = null;
        RealExpr sumOfPrtTime = null;
        
        for(int i = 0; i < 8; i++) {
            slotStart[i] = ctx.mkRealConst(this.name + "SlotStart" + i);
            slotDuration[i] = ctx.mkRealConst(this.name + "SlotDuration" + i);
        }
        
        for(FlowFragment f : this.flowFragments) {
            
            for(int i = 1; i <= 8; i++) {
                solver.add(
                    ctx.mkImplies(
                        ctx.mkEq(
                            f.getFlowPriority(),
                            ctx.mkInt(i)
                        ),
                        ctx.mkAnd(
                            ctx.mkEq(
                                slotStart[i-1],
                                cycle.slotStartZ3(ctx, f.getFlowPriority())
                            ),
                            ctx.mkEq(
                                slotDuration[i-1],
                                cycle.slotDurationZ3(ctx, f.getFlowPriority())
                            )
                        )
                    )
                );
            }
            
            
        }
        
        for(int i = 1; i<=8; i++) {
            firstPartOfImplication = null;
            
            for(FlowFragment f : this.flowFragments) {
                if(firstPartOfImplication == null) {
                    firstPartOfImplication = ctx.mkNot(ctx.mkEq(
                                                f.getFlowPriority(),
                                                ctx.mkInt(i)
                                             ));
                } else {
                    firstPartOfImplication = ctx.mkAnd(firstPartOfImplication, 
                                             ctx.mkNot(ctx.mkEq(
                                                 f.getFlowPriority(),
                                                 ctx.mkInt(i)
                                             )));
                } 
            }
            
            solver.add(
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
        
        
        solver.add(
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
     * [Method]: setupSchedulingRules
     * [Usage]: Calls the set of functions that will set the z3 rules
     * regarding the cycles, time slots, priorities and timing of packets.
     * 
     * @param solver        z3 solver object used to discover the variables' values
     * @param ctx           z3 context which specify the environment of constants, functions and variables
     */
    public void setupSchedulingRules(Solver solver, Context ctx) {
                
        setUpCycleRules(solver, ctx);
        
        /*
         * Differently from setUpCycleRules, setupTimeSlots and setupDevPacketTimes
         * need to be called multiple times since there will be a completely new set
         * of rules for each flow fragment on this port.
         */
        
        /*
        System.out.print(this.name);
        System.out.print(" - ");
        System.out.println(this.flowFragments.size());
        */
        
        for(FlowFragment flowFrag : this.flowFragments) {
            
            setupTimeSlots(solver, ctx, flowFrag);
            
            setupDevPacketTimes(solver, ctx, flowFrag);
            
        }
        
        if(flowFragments.size() > 0) {
            setupBestEffort(solver, ctx);
        }
        
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
        
        return flowFrag.getDepartureTimeZ3(Integer.parseInt(index.toString()));
        
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
     * @param index         Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the arrival time of the desired packet
     */
    public RealExpr departureTime(Context ctx, int auxIndex, FlowFragment flowFrag){
        return flowFrag.getDepartureTimeZ3(auxIndex);
        
        /**
        IntExpr index = ctx.mkInt(auxIndex);
        
        return (RealExpr) ctx.mkITE( 
                ctx.mkGe(index, ctx.mkInt(1)), 
                ctx.mkAdd(
                        flowFrag.getDepartureTimeZ3(auxIndex),
                        ctx.mkMul(flowFrag.getPacketPeriodicity(), index)
                        ), 
                flowFrag.getDepartureTimeZ3(auxIndex));
        /**/
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
     */
    public RealExpr arrivalTime(Context ctx, IntExpr index, FlowFragment flowFrag){
       
       /*
        *  The arrival time of this index from the given flow fragment is 
        *  equal to the its departure time + time to travel
        */
       return (RealExpr) ctx.mkAdd(
                      departureTime(ctx, index, flowFrag),
                      timeToTravelZ3
                      );
    }
    
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
        
        return (RealExpr) ctx.mkAdd(
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
     */
    public RealExpr scheduledTime(Context ctx, IntExpr index, FlowFragment flowFrag){
        RealExpr devT3 = ctx.mkRealConst(flowFrag.getName() + "ScheduledTime" + index.toString());
        
        return (RealExpr) devT3;
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
     * @param index         Index of the packet of the flow fragment as an integer
     * @param flowFrag      Flow fragment that the packets belong to
     * @return              Returns the z3 variable for the scheduled time of the desired packet
     */
    public RealExpr scheduledTime(Context ctx, int auxIndex, FlowFragment flowFrag){
        IntExpr index = ctx.mkInt(auxIndex);
        
        RealExpr scheduledTime = ctx.mkRealConst(flowFrag.getName() + "ScheduledTime" + index.toString());
        
        return (RealExpr) scheduledTime;
    }
    

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
            
            for(int i = 0; i < Network.PACKETUPPERBOUNDRANGE; i++) {
                prevPacketST = (RealExpr)
                        ctx.mkITE(
                            ctx.mkAnd(
                                ctx.mkEq(auxFrag.getFlowPriority(), f.getFlowPriority()),
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
    
    
    
    /*
     * GETTERS AND SETTERS:
     */
    
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
    
    public float getGbSize() {
        return gbSize;
    }

    public void setGbSize(float gbSize) {
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
       
}
