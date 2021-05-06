## Table of Contents

- [Generating topologies](#generating-topologies)

## Generating topologies

To aid in the generation of topologies for the testing of TSNsched, a generator of Java files containing the specification of a network was created. It can be found in the folder "ScenarioGenerator" of this repository.

Basically, given certain properties of a network as variables, they can be set in order to generate a topology according to the user's needs. The number of devices, switches, flows and constructor parameters are set, and then the file is written.

Currently, the number of nodes and subscribers is used in the following pattern. To create a small flow (3 switches in the path tree and 5 subscribers), the value of the configuration variable is 1. To create a medium flow (5 switches in the path tree and 10 subscribers), the value of the configuration variable is 2. To create a large flow (7 switches in the path tree and 10 subscribers), the value of the configuration variable is 3.

Firstly, a publisher device is picked from the pool of devices and it is made the root node, then the switch that it connects to is added to the path tree. From now own, every node in the path tree can have randomly up to 2 children nodes that will be switches picked from the mesh network. While the number of switches in the tree is smaller the *numberOfNodes* variable (which represents the number of switch nodes in the tree), branches will be created by level. This way, there can be at most a difference of one between the size of the biggest branch and the smallest branch. At this point, a number of devices that can go up to the specified number of subscribers will be equally divided by the switches in the end of the branches.

Even though the variation of flows in a generated file isn't too great (as to avoid creating completely different scenarios with similar configuration), the topologies created by this tool can be really complex to be solved.

To run the tool, the user must set the value of the variables according to the desired topology and then run the following commands on the ScenarioGenerator folder of this repository:

```
javac *.java
java ScenarioGenerator
```

The output file (GeneratedCode.java which contains the topology) will be generated within the same folder.

<!--
### Repository files:

|  File  |  Description  |
| ------ | ------ |
|Z3Code/scheduler-scenario1.z3|Z3 modeling of a simple scheduler (1 sender, 1 switch, 1 receiver) scenario with dynamic priorities and time slots|
|Z3Code/scheduler-FTS|Z3 modeling of a simple scheduler scenario with fixed time slots|
|Z3Code/scheduler-simple|Z3 modeling of a simple scheduler scenario with one time slot|
|Documents/Scenarios|PDF document containing the formal specification of two TSN scenarios|
|src/*|Files used by the Java project|

### Execution instructions for the Z3 code
* Open the file with the desired modeled scenario  
* Copy the content of the file
* Load the [Z3 website][z3]
* Paste the copied code on the website editor
* Press the "run" button
* The output will be printed bellow the editor
-->

