# TSNsched

> TSNsched uses the [Z3 theorem solver](https://github.com/Z3Prover/z3) to generate traffic schedules for [Time Sensitive Networking (TSN)](https://en.wikipedia.org/wiki/Time-Sensitive_Networking), and it is licensed under the [GNU GPL version 3 or later](License).

This repository is a result of research conducted at [fortiss](https://www.fortiss.org/en/) and the Federal University of Paraíba (UFPB) to develop a Time-Aware Shaper for TSN systems. The theoretic basis of this implementation has been published in a paper called [**TSNsched: Automated Schedule Generation for
Time Sensitive Networking**](Academic%20work/FMCAD%202019%20-%20TSNsched:%20Automated%20Schedule%20Generation%20for%20Time%20Sensitive%20Networking). The general idea of TSNsched was also discussed in depth [**a Master's dissertation of the same title**](Academic%20work/M.Sc%20Dissertaition%202020%20-%20TSNsched:%20Automated%20Schedule%20Generation%20for%20Time%20Sensitive%20Networking).


## Table of Contents

- [Quickstart Guide](#quickstart-guide)
- [Requirements](#requirements)
- [Command Line Usage](#command-line-usage)
  * [The Input](#the-input)
  * [Executing the program](#executing-the-program)
  * [The output](#the-output)
- [Using as a Library](#using-as-a-library)
  * [Setting up the network](#setting-up-the-network)
  * [Executing the program](#executing-the-program-1)
  * [The output](#the-output-1)
- [Generating topologies](#generating-topologies)
  * [Repository files:](#repository-files-)
  * [Execution instructions for the Z3 code](#execution-instructions-for-the-z3-code)
- [Simulation Parser](#simulation-parser)
  * [Generating Simulation Files](#generating-simulation-files)
  * [Running the Simulation](#running-the-simulation)
- [Overview of Classes](#overview-of-classes)
- [Frequently Asked Questions](#frequently-asked-questions)

## Quickstart Guide


On Linux run the shell script as root to set up all necessary dependencies. The script will also generate an example schedule.

```
sudo ./install-dependencies.sh
```

If you already have all dependencies installed, run the following commands for an example schedule. 

```
cd Script/
./generateSchedule.sh example.java
```

[example.java](Script/example.java) describes a network topology (10 switches, 50 devices) with one small flow (4 subscribers and 3 switches in the path tree):

    P -> SW1 -> SW2 -> Sub1
         |       |
         |       V
         |       Sub2
         V        
         SW3 -> Sub3
         |
         V
        Sub4


TSNsched writes the generated TSN schedule to the [output directory](Script/output).

The total execution time, average latency and average jitter of the topology can be found at the end of the [output/example.java.out](Script/output/example.java.out) file.

You can find the network topologies described in the submitted paper in [TestCases](TestCases/).

You can also generate other topologies using our flow generator. [Generating topologies](#generating-topologies) explains how to use this generator.


## Requirements

* Java Version 1.8.0_181
* Z3 package version 4.8.0.0
* GNU bash version 4.4.19(1)-release (x86_64-pc-linux-gnu)
* Eclipse IDE 4.8.0


## The Input

Internally, TSNsched is capable of perceiving the network as a set of devices, switches and data streams (commonly referred as flows) and these elements can be represented with a JSON object. Example topologies containing possible configurations for TSNsched can be found in the [scripts folder](Script/).

The description of the fields of the json elements describing the network topology (input) is shown bellow:

### Device
- name: name of the device;
- defaultPacketSize: (optional) default size of every frame sent by this device. Can be overridden by the packetSize variable on the flow object;
- defaultPacketPeriodicity: (optional) default interval between frame sendings expressed as time. Can be overridden by the packetPeriodicity variable in the flow object;
- defaultHardConstraintTime: (optional) default maximum latency of every frame sent by this device expressed as time. Can be overridden by the hardConstraintTime variable in the flow object;
- defaultFirstSendingTime: (optional) default moment in time in which the first packet of the device is sent expressed as time. Can be overridden by the firstSendingTime variable in the flow object. If the device is the source of multiple flows and the flows do not override the first sending time with different values, or if the transmission of its first packets overlap, this variable will be ignored and a new value for it will be given as output for both flows;

### [INPUT]

### TSNSwitch
- name: name of the switch;
- defaultTimeToTravel: (optional) default time taken to travel between the port of the switch and the egress queue of the node it connects to. Can be overridden by the timeToTravel variable of the object. Expressed as time;
- defaultPortSpeed: (optional) default transmission speed of the ports of the switch. Can be overridden by the portSpeed variable of the Port object. Expressed as size per time;
- defaultGuardBandSize: (optional) default size of the guard bands in the port. Can be overridden by the guardBandSize variable in the Port object. Expressed as size;
- defaultScheduleType: (optional) default schedule type of all the ports. Can be overridden by the scheduleTypeVariable in the Port object;
- defaultSlotArrangementMode: (optional) default slot arrangement mode of all the ports of the switch. It can be overridden by the slotArrangementMode variable on the Port;
- port* [connectsTo] 
    - name: name of the port element;
    - connectsTo: name of the node it connects to;
    - timeToTravel: (optional) time taken to travel between the port of the switch and the port of the node it connects to. Overrides the default of the switch. Expressed as time;
    - guardBandSize: (optional) size of the guard bands in the port. Overrides the default guard band size of the switch. Expressed as size;
    - maximumSlotDuration: (optional) maximum size of all the transmission windows (space of time between the egress gate opening and closing on a port) of the port. Overrides the default maximum slot duration of the switch. Expressed as time;
    - cycleStart: (optional) first cycle start of the cycle of the port;

### Flow 
- name: name of the flow
- fixedPriority: (optional) a boolean variable which, if set to true, will force the flow to have the same priority over all its hops. If set to false, the priority of the flow can change from hop to hop, considering that the network has support for this feature (based on the 802.1Qci standard);
- priorityValue: (optional) an integer variable where its value ranges from -1 to 7. If it is -1, this variable is given as output. If it is any other value, as long as the fixedPriority variable is set to true, the scheduler will force the flow to be scheduled on the priority number specified by this variable;
- firstSendingTime: (optional) moment in time in which the first frame sent in this stream of frames is sent. Overrides the defaultFirstSendingTime of the Device object. If the source device is also the source of another flow, and the flows do not override the first sending time with different values, or if the transmission of its first packets overlap, the input value for this variable will be ignored and a new value for it will be given as output. Expressed as time;
- packetPeriodicity: (optional) interval between frame sendings expressed as time. Overrides the defaltPacketPeriodicity variable in the Device object;
- hardConstraintTime: maximum latency of the packets of that flow;
- maximumJitter: (optional) maximum variation of the latency of the packets of that flow;
- sourceDevice: name of the source device
- endDevice* [name] - List of names of end devices;
    - name: name of end device;
- hop* [nextNodeName]
    - currentNodeName: name of the node from where the packet departs on the hop;
    - nextNodeName: next node in the frame’s path;
   
### [OUTPUT]

### Flows
- name: name of the flow
- averageLatency: average latency of all the packets of the flow
- jitter: average variation of all the packets of the flow
- firstSendingTime: moment in time where the first packet of the flow is sent
- priority: (if fixed priority is true in the input) priority of the packets of the flow
- hops* (used to specify the priority of each hop, if the flow has no fixed priority)
     - currentNodeName: name of the current node in the path
     - nextNodeName: name of the next node in the path
     - priority: priority of the stream in the egress port of the current node

### Switch
- name: name of the switch
- ports* [name]
      - cycleDuration: duration of the cycle in the port
      - firstCycleStart: moment in time where the first cycle starts in the port  
      - prioritySlotData*
            - priority: priority number of the transmission window
            - slotsData*
                  - slotDuration: duration of the transmission window     
                  - slotStart: moment in time where the transmission window starts



## Command Line Usage

Primarily, TSNsched is compiled as a executable .jar file. This file can be found in the [libs](libs/) folder under the name of TSNsched.jar.

To execute TSNsched, please run the following command:

     java -jar TSNsched.jar INPUT_FILE_NAME

To use the provided sample file as input, replace INPUT_FILE_NAME with input.json.

[comment]: <> (To use the functionality provided in this deliverable, please run the command above with the “-useIncremental” parameter.)

By default, TSNsched will generate a file titled output.json, which contains the generated schedule organized into json elements. These elements have the necessary information to deploy the schedules in the topology hardware.

TSNsched supports multiple parameters command line execution. The parameters and their description can be seen below:

   - exportModel: Exports the SMT-solver model generated by TSNsched;
   - generateSimulationFiles: Exports files used as input for simulating the generated schedule in omnet++; 
   - serializeNetwork: Serializes the network configuration for future use. The schedule can be loaded by using the “-loadNetwork” parameter;
   - loadNetwork: Used to load the serialized topology into the scheduler;
   - enableConsoleOutput: Enables console output for debugging and visual feedback of the tool;
   - enableLoggerFile: Generates a readable file containing the information of the schedule;
   - disableJSONOutput: Used to stop the tool from generating the JSON output;
[comment]: <> (   - useIncremental: When used, enable the incremental scheduling approach to be used.)

Alternatively, this project accompanies a script to execute the scheduler with the necessary configuration for exporting human readable output, and the files used in this approach are stored in the folder [Script](Script/) in this repository. They can be downloaded and used separately.

If the user is not interested in building his own network, we also make available a topology generator, discussed later in this file. The output of this generator is already in the format accepted by the execution script. Samples generated by this tool can be found in the folder "TestCase" and are indentified by the .java extension. We discourage the usage of the input for TSNsched as java files, as it is gradually becoming deprecated in favor of the json input.

This file must be placed inside the folder "Script". The name of the file does not matter, as it will be an input on the command line.

For the script usage, a script was developed in order to compile the Java file containing the network topology, execute it and handle the input and output files.

Once the input file is placed in the same folder of the script, the user must execute the script giving the java file containing the topology as an argument. Given that the name of the file is "example.json", the command will look like the following:

```
./generateSchedule.sh example.json
```

For practical reasons, the given file will be duplicated, renamed, parsed by the script in order to adapt the code. Then, the files will be compiled and executed with references to the Z3 and TSNsched libraries, placed on the subfolder "libs". The 2 output files will be generated and placed on the folder "output" under the the same name of the argument given in the execution of the switch, but now with the extra extensions .out and .log.   

### Complimentary output

The output of this process can be found in the "output" subfolder. If the network topology was specified on a file called "topology.json", then the user should be able to find two new files in the output folder called "topology.java.out" and "topology.java.log", if there was no problem executing the script.

The files with the extension .out contain the printed model generated by Z3 with the extra output created by the user (optional).

The files with the extension .log contain the information about the topology, as well as the Z3 values generated for the properties of the network. These files will be divided in a list of switches and a list of flows. 

The list of switches contains individual information about each switch (such as transmission time and time to travel) and its ports (virtual index, first cycle start and duration for debugging purposes. Mostly redundant). 

The list of flows contains individual information about each flow. Here, the user can check the flow fragments to retrieve the values of the priority of the flow on a certain switch, the slot start and duration of that flow, and the arrival, departure and scheduled times of each of the packets that goes through the switch covered by this fragment of the flow. 

Currently, the scheduler is building the schedule for 5 packets sent by each flow, which can be configured for different settings. Due to this, the user might indentify a pattern on the log files. A index of the packet between parenthisis can be seen followed by the departure, arrival and scheduled times. After this, a dashed line will be printed, separating it from the information about the next packet of the same flow. 

## Using as a Library

This tool also can be imported as a library allowing the user to aggregate TSNsched functionalities to other projects. With this, not only the topology can be handled as the developer wishes, the values generated by the scheduler will be stored in the cycle and flow objects in the program, allowing users to manipulate data without waiting for an output of a program external to their projects.


### Setting up the network

After adding the TSNsched and Z3 packages to the Java build path, one only needs to import the classes in order to be able to make use of it:

```
import schedule_generator.*;
```

With this, components of the network can be created:

```
// Creating a device
Device dev = new Device(float packetPeriodicity,  //  Periodicity of the packet
                        float firstT1Time,        //  First sending time of the device (Check toZ3 method on the device object to see if it is being used)
                        float hardConstraint,     //  Maximum latency tolerated by this device (Hard constraint)
                        float packetSize);        //  Size of packet sent by the device
                
// Creating a switch
TSNSwitch switch = new TSNSwitch(String name,   	 // Identifier of the switch
				       float timeToTravel,     // Time taken to travel on the medium connected to this switch
			         float portSpeed,        // Transmission speed of the port
			         float gbSize)           // Size of the guardband used in the port in time units
				 
// Creating a cycle
Cycle cycle = new Cycle(float maximumSlotDuration);   // Maximum duration of a time window of the cycle

// With the cycle, create ports. 
// First parameter is the device that is being connected to the switch, second is the cycle of the port.
switch.createPort(Device deviceA, Cycle cycle1); 
switch.createPort(Switch switchB, Cycle cycle2); 

// Creating a unicast flow:
Flow flow = new Flow(Flow.UNICAST);

// Setting start device, path and end device of a unicast flow
flow.setStartDevice(Device devA);
flow.addToPath(Switch switchA);
flow.addToPath(Switch switchB);
flow.setEndDevice(Device devB);
	

// Creating a publish subscribe flow:
Flow flow = new Flow(Flow.PUBLISH_SUBSCRIBE);
flow.setStartDevice(Device devA);
// Since now the path can be a tree, the source must also be informed
flow.addToPath(Device devA, Switch switchA); // Adding path from devA to switchA
flow.addToPath(Switch switchA, Switch switchB);
flow.addToPath(Switch switchB, Switch switchC);
flow.addToPath(Switch switchB, Switch switchD);
flow.addToPath(Switch switchC, Device devB);
flow.addToPath(Switch switchD, Device devC);


// Creating and populating a network (Giving switches and flows to it):
Network net = new Network(float jitterUpperBoundRange); // Creating a network giving the maximum average jitter allowed per flow
net.addDevice(Device devA);
net.addDevice(Device devB);
net.addDevice(Device devC);
net.addSwitch(Switch switchA); 
net.addSwitch(Switch switchB); 
net.addSwitch(Switch switchC); 
net.addFlow(Flow flowA); 
net.addFlow(Flow flowB); 

```


### Executing the program

The user must add both Z3 and TSNsched packages to the classpath of the project. These two files can be found in the "libs" folder of this repository.

Most of the sofisticated IDEs can do this just by adding external libraries as JAR files on the configuration of the projects.

If compiling the project in the command line, do not forget to add the libraries manually or set them in the Java PATH environment variable.

After setting up the network, the user must now call the method for generating a schedule:

```
// Generating a schedule:
ScheduleGenerator scheduleGenerator = new ScheduleGenerator();
scheduleGenerator.generateSchedule(Network net); // The network is the input for the schedule generation
```
### The output

By default, TSNsched will generate a .json output file containing the information about the flows, its individual packets and the gate control list information of the switches. The output is structured as follows:


### [Flows]

- name: name of the flow
- averageLatency: average latency of all the packets of the flow
- jitter: average variation of all the packets of the flow
- firstSendingTime: moment in time where the first packet of the flow is sent
- priority: (if fixed priority is true in the input) priority of the packets of the flow
- hops* (used to specify the priority of each hop, if the flow has no fixed priority)
     - currentNodeName: name of the current node in the path
     - nextNodeName: name of the next node in the path
     - priority: priority of the stream in the egress port of the current node

### [Switch]
- name: name of the switch
- ports* [name]
      - cycleDuration: duration of the cycle in the port
      - firstCycleStart: moment in time where the first cycle starts in the port  
      - prioritySlotData*
            - priority: priority number of the transmission window
            - slotsData*
                  - slotDuration: duration of the transmission window     
                  - slotStart: moment in time where the transmission window starts


If the logging functionality is enabled on input, a "log.txt" file must be generated within the project folder. This file contains the information about the topology, as well as the Z3 values generated for the properties of the network (such as cycle start and duration, priorities and packet times).

If importing TSNsched in a library in your project, it is possible to return numeric data to the user accessing the topology object as follows:

```
// Retreiving the departure time from a packet in a publish subscribe flow 
flow.getDepartureTime(Device targetDevice,     // Destination of the packet. One of the subscribers 
                      int switchNum,           // Index of the switch in the path
                      int packetNum);          // Index of the packet in the sequence

// Retreiving the arrival time from a packet in a publish subscribe flow 
flow.getArrivalTime(Device targetDevice,       // Destination of the packet. One of the subscribers 
                    int switchNum,             // Index of the switch in the path
                    int packetNum);            // Index of the packet in the sequence

// Retreiving the scheduled time from a packet in a publish subscribe flow 
flow.getScheduledTime(Device targetDevice,       // Destination of the packet. One of the subscribers 
                      int switchNum,             // Number of the switch in the path
                      int packetNum);            // Index of the packet in the sequence

// Retrieving the average latency and average jitter of a flow, respectively:
flow.getAverageLatency();
flow.getAverageJitter();

// Retrieving the cycle duration and cycle start of a flow:
cycle.getCycleDuration();
cycle.getCycleStart();

// Retrieving the index of priorities used:
cycle.getSlotsUsed();

// Retrieving the priority slot duration and priority slot start:
cycle.getSlotStart(int prt);        // Index of a priority
cycle.getSlotDuration(int prt);     // Index of a priority

```

## Generating topologies

Check the full description on how to generate arbitrary TSNsched input topologies [here](ScenarioGenerator/README.md).

## Simulation Parser

TSNsched offers means of validation through the translation of the values contained in the Network object created in the scheduling generation process. Using the Network object, the parser creates the necessary files for the [NeSTiNg](https://gitlab.com/ipvs/nesting) simulation model that uses the [OMNeT++](https://omnetpp.org/) and [INET Framework](https://inet.omnetpp.org/).

### Generating Simulation Files

The generation of simulation files is fully automated. At the end of the default TSNsched scheduling generation, the creation of the necessary files for the simulation begins, being these files:

* Network Description (.ned)
* Initialization (.ini)
* Port Scheduling (.xml)
* Routing (.xml)
* Traffic Generator (.xml)

All of these files can be found in the folder named [nestsched](nestSched/).

### Running the Simulation

To run the simulation and validade the generated scheduling, it's necessary to have NeSTiNg, a simulation model for Time Sensitive Networking that currently uses OMNeT++ version 5.5.1 and INET version 4.1.2.

With the tools properly installed, it is now necessary to make some changes to the source code of NeSTiNg and INET so that it is possible to generate the analysis signals that we need and calculate the latency of communication between devices.

The first change to be made is in the INET Framework, more precisely in the [Ethernet.h](https://github.com/inet-framework/inet/blob/master/src/inet/linklayer/ethernet/Ethernet.h) file. In this file, it is necessary to change the value of the constant that defines [INTERFRAME_GAP_BITS](https://github.com/inet-framework/inet/blob/master/src/inet/linklayer/ethernet/Ethernet.h#L36) from 96 to 0. The Interframe Gap is the minimum pause necessary for the receiver to be able to make clock recoveries, allowing it to prepare itself for receiving the next packet. This change is necessary because TSNsched does not take Interframe Gap into account when generating the network schedule.

Next we need to change the [QueuingFrames.h](https://gitlab.com/ipvs/nesting/-/blob/master/src/nesting/ieee8021q/queue/QueuingFrames.h) file in the NeSTiNg. In this file, the [last line of the matrix standardTrafficClassMapping](https://gitlab.com/ipvs/nesting/-/blob/master/src/nesting/ieee8021q/queue/QueuingFrames.h#L54) has to follow the ascending order from 0 to 7, so that the packets can be routed correctly according to the TSNsched configuration.

And finally we need to change the [VlanEtherTafGenSched.cc](https://gitlab.com/ipvs/nesting/-/blob/master/src/nesting/application/ethernet/VlanEtherTrafGenSched.cc) and [VlanEtherTafGenSched.h](https://gitlab.com/ipvs/nesting/-/blob/master/src/nesting/application/ethernet/VlanEtherTrafGenSched.h) files. In these files, we need to change three things: (i) we need to track the number of packets sent, so the maximum number of packets determined by the generated schedule are followed. (ii) we need to change the packet name, putting the flowId in the beginning of the name, so we can easily track that information later. (iii) and we need to create and initialize the flowId signals and generate them when a packet is received.

With all these changes made, now you just have to generate the network scheduling with TSNsched, copy the [nestsched folder](nestSched/) into the [NeSTiNg examples folder](https://gitlab.com/ipvs/nesting/-/tree/master/simulations/examples) and run the simulation in the OMNeT++.


## Overview of Classes

Check the full description of TSNsched classes [here](src/README.md).

## Frequently Asked Questions

Check the full TSNsched's FAQ [here](FAQ.md).
