## Table of Contents

- [Overview of Classes](#overview-of-classes)
  * [Device](#device)
  * [Flow](#flow)
  * [FlowFragment](#flowfragment)
  * [Switch](#switch)
  * [TSNSwitch](#tsnswitch)
  * [Network](#network)
  * [PathNode](#pathnode)
  * [PathTree](#pathtree)
  * [Cycle](#cycle)
  * [Port](#port)
  * [ScheduleGenerator](#schedulegenerator)

## Overview of Classes

A brief presentation of the project classes and its quirks.

### Device

The Device class represents a start or end device in a TSN flow. All the properties of device nodes in the network are specified here. These properties are rather trivial to understand but they are build the core of the constraints of a flow. Here, the user can specify the packet periodicity of the flow and the hard constraint (maximum allowed latency). 

### Flow

This class specifies a flow (or a stream, in other words) of packets from one source to one or multiple destinations. It contains references for all the data related to this flow, including path, timing, packet properties, so on and so forth. The flows can be unicast type or publish subscribe type flows.

In a more in deph perspective, each flow will later be broken into "smaller flows", called flow fragments. This class will also store the reference to them in a simple ArrayList (for unicast flows) or inside a PathTree object (for publish subscribe flows).

### FlowFragment

This class is used to represent a fragment of a flow. Simply put, a flow fragment represents the flow it belongs to regarding a specific switch in the path. With this approach, a flow, regardless of its type, can be broken into flow fragments and distributed to the switches in the network. It holds the time values of the departure time (leaving the previous node), arrival time (arriving in the current node) and scheduled time (leaving the current node) of packets from this flow on the switch it belongs to. Since these times are specified as Z3 objects, there is no need to store copies of them, just the reference.

This approach allows the user to have a more encapsulated code, since it doesn't matter the type of flow being used here, the user can simply break the flow of packets into nuclear streams (a stream that only covers one hop), and visualize the fragment of a flow as a link in a chain.

FlowFragment objects store information about the current and next nodes in its path and also the departure time, arrival time, scheduled time of the packets that go through it. It is important to have in mind that the departure, arrival and scheduled times stored by FlowFragment objects are float values, not Z3 variables. The Z3 variable for these values can be retrieved through the port or the switch that this fragment goes through.


### Switch
Contains most of the properties of a normal switch that are used to build the schedule. Since this  scheduler doesn't take in consideration scenarios where normal switches and TSN switches interact, no Z3 properties had to be specified in this class. 
 
It is currently used as parent class for TSNSwitch. Can be used to further extend the usability of this project in the future.

### TSNSwitch

This class contains the information needed to specify a switch capable of complying with the TSN patterns to the schedule. Aside from part of the Z3 data used to generate the schedule, objects created from this class are able to organize a sequence of ports that connect the switch to other nodes in the network.

TSNSwitch objects also can reference the Z3 variables for the departure, arrival and scheduled time of FlowFragments.  

### Network

Using this class, the user can specify the network topology using switches and flows. The network will be given to the scheduler generator so it can iterate over the network's flows and switches setting up the scheduling rules.

In this current implementation, the upper bound jitter variation is specified in the Network, making it uniform for all flows added to the topology.

### PathNode

In a publish subscribe flow, contains the data needed in each node of a pathTree. Since a publish subscribe flow path can be seen as a tree, a single node on that tree is a PathNode.

Can reference a father, possesses an device or switch, a list of children and a flow fragment for each of the children in case of being a switch.

### PathTree

Used to specify the path on publish subscribe flows. Has a reference to the PathNode root, which contains the starting device, and also references to the leaves, which contain the destinations of the publisher messages.

It is basically a tree of PathNodes with a few simple and classic tree methods. 

### Cycle

The Cycle class represents the cycle of a TSN switch. A cycle is a time interval with a specific duration where time windows can be distributed according to constraints to prioritize critical traffic. During these time windows, the gate of the respective priority queue will be open. Each cycle has a a start, a duration and a sequence of time windows (priority slots).

In this project implementation, since a set of priority queues is given for every port in a switch, every port has a cycle, but the cycle start and duration is the same for every port.

After the specification of its properties through user input, the toZ3 method can be used to convert the values to Z3 variables and query the unknown values. 
 
There is no direct reference from a cycle to its time slots. The user must use a priority from a flow to reference the time window of a cycle. This happens because of the generation of Z3 variables. 
 
For example, if I want to know the duration of the time slot reserved for the priority 3, it most likely will return a value different from the actual time slot that a flow is making use. This happens due to the way that Z3 variables are generated. A flow fragment can have a priority 3 on this cycle, but its variable name will be "flowNfragmentMpriority". Even if Z3 says that this variable's value is 3, the reference to the cycle duration will be called "cycleXSlotflowNfragmentMpriorityDuration", which is clearly different from "cycleXSlot3Duration".
 
To make this work, every flow that has the same time window has the same priority value. And this value is limited to a maximum value *numOfSlots*. So, to access the slot start and duration of a certain priority, a flow fragment from that priority must be retrieved. This also deals with the problem of having unused priorities, which can end up causing problems due to constraints of guard band and such.

### Port

This class is used to implement the logical role of a port of a switch for the scheduler. The core of the scheduling process happens here. Simplifying the whole process, the other classes in this project are used to create, manage and break flows into smaller pieces. These pieces are given to the switches, and from the switches they will be given to their respective ports according to the path of the flow.
 
After this is done, each port now has an array of fragments of flows that are going through them. This way, it is easier to schedule the packets since all you have to focus are the flow fragments that might conflict in this specific port. The type of flow, its path or anything else does not matter at this point.

### ScheduleGenerator

Used to generate a schedule based on the properties of a given network through the method generateSchedule. Will create a log file and store the timing properties on the cycles and flows.
