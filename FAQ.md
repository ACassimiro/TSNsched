# TSNsched Frequently Asked Questions

###### How do I set the Cycle Start time of the switches to be the same? 
By default, TSNsched already makes the cycle start time of the switches to be the same. That's possible thanks to the constraint in the line [226](https://github.com/ACassimiro/TSNsched/blob/69fc3e7b88d6f06d988553727e4bd4399cf37cbe/src/schedule_generator/TSNSwitch.java#L226) of the class TSNSwitch.java. If you’re trying to set a specific value to the cycle start time, you need to change the constraint in the line [234](https://github.com/ACassimiro/TSNsched/blob/69fc3e7b88d6f06d988553727e4bd4399cf37cbe/src/schedule_generator/TSNSwitch.java#L234) of the same file mentioned before, more specifically, the value of the line [236](https://github.com/ACassimiro/TSNsched/blob/69fc3e7b88d6f06d988553727e4bd4399cf37cbe/src/schedule_generator/TSNSwitch.java#L236). By default, the cycle start time of all switches are defined as 0. 

###### How the Departure Time and Arrival Time works? 
The Departure Time is equal to the time where the last bit of the packet has left the previous node. The Arrival Time is equal to the time where the last bit of the packet has arrived in the current node. 

###### Why do the Flow Fragments have different priorities? 
By default, TSNsched considers that the input network is 802.1Qci compliant. According to "S. S. Craciunas, R. S. Oliver, and T. C. AG. An overview of scheduling mechanisms for time-sensitive networks. Proceedings of the Real-time summer school LÉcole dÉtéTemps Réel (ETR), 2017", the IEEE 802.1Qci standard also plays an important part in the scheduling approach used by TSNsched (if configured to be used), as it provides the necessary grounds to identification of streams and modification of packet headers performed on switches. 
Although it is not necessary, this functionality can improve the efficiency of TSNsched for generating schedules. This can be configured by using the method setFixedPriority(true) in the flow objects of your network description file before running the scheduleGenerator.generateSchedule(net) method. 

###### How do I set the priority of a Flow? 
By default, TSNsched automatically assigns the priority of a flow when it generates the schedule of the network. But you can set a specific value to a flow by using the method setPriorityValue(int value) in the flow objects of your network description file before running the scheduleGenerator.generateSchedule(net) method. 

###### Why don't my changes in the code take effect? 
If you’re running TSNsched on a command prompt, you will have to make the changes in the code and generate a new JAR file from the project. After this, you can remove the files in the libraries folder (except the z3 library) and put the new generated JAR file there (and finally run the script). 
If you have exported TSNsched to an IDE and you're running the project instead of running the script (and consequently using TSNsched as a library), you would only need to change the pieces of code you need. Exporting TSNsched to an IDE allows you to modify and configure TSNsched more quickly, as you don't have to generate JAR files for every change in the code that you wish to perform.
