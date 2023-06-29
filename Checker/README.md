
# TSN-logs-checker
The <a href="https://github.com/ACassimiro/TSNsched">TSNSched</a> provides three outputs files: .log, .out and a JSON file. The log and JSON files bring the same information but in some different ways as they are structured differently. That being said, it's possible to generate unexpected results that don't raise errors at runtime. Checking either the logs or the JSON line by line might be infeasible due to the potentialy great extension of the file, so we made a Checker to validate the output of those files. This checker receives both of them as input that have an order to be passed, the json file as the first argument followed by the log file. Those files however must meet the required format as they were made to find patterns and specific nomenclatures to make its validation, so changes to the spelling causes this checker to no longer work appropriately. Notice that this project is constantly being updated, and we intend to extend this accordingly to the needs, so we may be adding more validations, features and increasing this documentation.

### Table of Contents

- [Criterias](#criterias)
  - [Typechecking value](#typechecking-value)
  - [Well formed hops ](#well-formed-hops)
  - [Consistent path nodes](#consistent-path-nodes)
  - [Transmission windows consistency](#transmission-windows-consistency)
  - [Table fo criteria](#table-of-criteria)
- [Categories](#categories)
  * [Sintax](#sintax)
    * [Data Type](#data-type)
    * [File Type](#file-type)
  * [Semantic](#semantic)
    * [Switches](#switches)
      * [Ports](#ports)
    * [Flow](#flow)
      * [Paths](#paths)
      * [Packet times](#packet-times)
 - [How to run](#how-to-run)

# Criterias

There are some topics we need to check. They were selected based on the following criterias:

- Criteria 1: All values of time(Departure, arrival and scheduled times) are positive (Typechecking-value)
- Criteria 2: Time of sent plus duration time of transmission must be equal to the scheduled time. (Well formed hops)
- Criteria 3: Consistent path nodes.
- Criteria 4: Each packet must be transmitted at its correspondent priority window.(Transmission windows consistency - Cycle)
- Criteria 5: Two packets can't be transmitted at same time at same port.(Transmission windows consistency – Priority Window)
- Criteria 6: Packets at the same port of same priority must be sent in arrival order(FIFO like).(Transmission windows consistency – Sent Order)

# Categories

Among the many validations we've made, it may become hard to track, so we considered an approach to categorize them in order to ease management and search for a specific issue. We then separated our validations into topics and subtopics, which are constantly growing as we develop more validations. It's divided into two major groups: semantic and syntactic. Syntactic category is about the syntactic part as for misspelling, while semantic is related to deeper validation of the data and what they represent because may all the data look good but hold a critical error.

![image](https://user-images.githubusercontent.com/52057929/152810672-e8107856-7116-404c-aa3c-d24552c08b0c.png)
<h6>Categories' Tree</h6>


# Syntax
Throughout the checker we make some validation which may be separated into two major groups, each with different purposes. We are assigning here all the syntactic validations. This checker receives two files from the user and the first step is to ensure all data meet the requirements such as data and file types to proceed with its validations as it's the basis for semantic validation. Syntactic errors are easier to be noticed as they raise attention as soon as spotted; for instance, some letters in the place of a number.

### Data Type
This subtopic refers to the validation of data type, as mentioned earlier, a letter in the place of a number, and other validations like that. The checker verifies the data type of information brought into the file to be validated to ensure they are the type they're meant to be.

### File Type
This subtopic refers to the validation of files type, just like the one before, but now aiming at the files only. It's already known by now that this checker receives two files, one being a JSON file and the other a log file that can be with the .log format extension or a simple text(.txt) extension. So we make a quick verification to ensure the file passed corresponds to these files.

# Semantic
For we have made verification of the syntactic of our file, now we are to face the semantic verification, this means the correctness of the data into a deeper point of view. The semantic differs from the syntax at adopting a more technical view of situation, so it may not be so clear to the user that doesn't comprehend the behavior of the TSNSched, or it may just pass unnoticed by even experienced users as the topology may be too large, which is the purpose of this project, that is, to help users make verifications in such a case. In short, semantic validations aims logical question, for instance, transmission of packet on a port and its transmission window.

## Switches 
This is a subtopic which validates the data found on a switch. But what does a switch hold? It holds the cycle duration of each switch, the priorities assigned to flows, and the TSN time slots allocated for each priority at each port of each switch.

### Ports
This subtopic refers to validations of the port's data to ensure they make sense. The ports contain some important data for TSNSched like the start of transmission of the packets, its cycle duration and the duration time of a transmission, that is, how long it took to be transmitted. These data are critical as they represent the main subject of the TSNSched as it proposes a solution to the scheduling problem of TSN. <a href="https://github.com/ACassimiro/TSNsched" target="_blank">Check it out</a> for more details about it.

## Flow
This is a topic which validates the flow data and questions related to it. You may find on a flow the average latency, jitter, data time of the packets and hops to end devices all inside the JSON file. The complete details of the fragments can be found on the log file. The data on the log file is the combination of data found of switches on JSON file and the flow on JSON file for each hop, so the data brought is completely detailed for each flow fragment. The flow then can be divided into a couple more specific subtopics, they're: paths and packet times. We discuss more about them in their respective subtopics.

### Paths
The flows contain data about the hops from the origin device to the end device. It describes each hop from the first device to the end device by presenting the current node, the destination node, and the priority of the hop. If you look at the log file,  you'll also see the data of each flow fragment in detail. So there may occur that a node may be missing or the scheduling inserts a node in the wrong place. We then made a validation to ensure the path to the end devices follows as they are supposed to on each flow.


### Packet Times
A flow is broken into fragments, and it also contains data of the packets. This data in the json file includes the time when it left the previous node, the time it arrived at the current node, and the time when it was sent to the next node. On the log file under the respective flow fragment the information is more detailed as it brings the current hop(the origin and destination), the priority of the fragment, and the information mentioned before: the arrival and departure times. This information is the core of TSNsched, they are critical for the deterministic network we are looking to accomplish, so we verify each of these data to ensure they are correct, and the schedule works as intended.

## How to run

There is a jar file inside the folder Checker, in a terminal just run the following command passing the path to the json file followed by the log file, the result will then been shown in the terminal:

```
java -jar Checker.jar [path to the .json file] [path to the .log file]
```

## Typechecking value

Below, it's an example of how the TSN outputs logs of time in the .log file. It brings data about the departure, arrival and scheduled for there can't be negative time in the real world, the Checker validates whether it happens or not for every time log.

```
(0) Fragment departure time: 41.0
(0) Fragment arrival time: 42.0
(0) Fragment scheduled time: 55.002
```

## Well formed hops

TSN schedules and controls the transmission of packets on the flows based on time, as it's a time-sensitive network, so time is the most important thing to be considered. It's critical that flows respect its schedule to minimize latency and jitter. It can be validated by adding slot start and the slot duration and compare to the scheduled time to validate it as show below:

```
Fragment slot start 0: 42.01 ; 4201/100
Fragment slot duration 0 : 13.0 ; 13
Fragment scheduled time: 55.01
```


## Consistent path nodes
During runtime, it might happen that TSN generates a path that is not supposed to exist or to be at a certain flow. There is a need, then, to check whether the paths are well-made or not, so this checker also validates it by comparing the path to a device with the hops data.


This is the path to a device called dev5, we hope the flow to follow as it says.
```
 Path to dev5: 
dev38, 
switch7(flow1Fragment1), 
switch0(flow1Fragment2), 
switch1(flow1Fragment3), 
dev5,
```

So we check the hops to see if it follows that flow and gets to where it's supposed to.

```
Fragment name: flow1Fragment1
        Fragment node: switch7
        Fragment next hop: switch0
```
```
Fragment name: flow1Fragment2
        Fragment node: switch0
        Fragment next hop: switch1
```
```
Fragment name: flow1Fragment3
        Fragment node: switch1
        Fragment next hop: dev5
```

## Transmission windows consistency
Maybe the most import subject of validations are the ports and windows of transmission. We check if there are two or more packets being transmitted at the same time on the same port, if each packet is being transmitted at its correspondent priority window and if the packet of same priority on the same port are being transmitted in order of arrival. Another thing to consider is that there is a maximum limit for sending packets, so if a packet is to be sent somewhere between 200 and 250 microseconds(50 is the maximum value allowed) it can't be sent after this interval, so we also need to check if a transmission is not overpassing its cycle. This information is brought in the json file.

This is how both start of transmission and transmission time are shown. We need to check if they start at the same time and if they don't overpass its cycle.
```
"cycleDuration": 500.0,
"name": "eth203",
"firstCycleStart": 0.0
"prioritySlotsData": 
[
  {
    "slotsData": [
      {
        "slotDuration": 0.576,
        "slotStart": 499.424	// slotStar + slotDuration <= cycleDuration
      }
    ],
    "priority": 0
  },
  {
    "slotsData": [
      {
        "slotDuration": 0.576,
        "slotStart": 489.568	// slotStar + slotDuration <= cycleDuration
      }
    ],
    "priority": 1
  }
]
```


<br />

## Table of criteria

| Criteria                                                     | Description                                                                                                      |    <br>Pass-case                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |    <br>non   pass-case                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|--------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|    <br>Type   checking value                                 |    <br>All values of time (Departure, arrival and scheduled times) are   positive                                |    <br>(0) Fragment departure time: 41.0<br>   <br>(0) Fragment arrival time: 42.0<br>   <br>(0) Fragment scheduled time:   55.002                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |    <br>(0) Fragment departure time: 41.0<br>   <br>(0) Fragment arrival time: <br>   <br>-42.0<br>   <br>(0) Fragment scheduled time:   55.002                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|    <br>Well   formed hops                                    |    <br>Sum of Time of sent and duration time of transmission must be equal to   the scheduled time.              |    <br>Fragment slot start 0: 42.01 ; <br>   <br>Fragment slot duration 0: 13.0 ; <br>   <br>Fragment scheduled time: 55.01                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |    <br>Fragment slot start 0: 42.01 ; <br>   <br>Fragment slot duration 0: 13.0 ; <br>   <br>Fragment scheduled time: 52.01                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|    <br>Consistent   path nodes                               |    <br>All paths to a certain device is consistent through the file                                              |    <br>"flows": [ <br>   <br>…<br>   <br>"hops": [<br>   <br>{<br>   <br>            "nextNodeName": "switch1",<br>   <br>            "currentNodeName": "switch9",<br>   <br>            "priority": 7<br>   <br>        },<br>   <br>        {<br>   <br>            "nextNodeName": "dev3",<br>   <br>            "currentNodeName": "switch1",<br>   <br>            "priority": 1<br>   <br>        },<br>   <br>…<br>   <br>]<br>   <br>---------<br>   <br>Flow List:<br>   <br>…<br>   <br>Path to dev3: dev29, switch9(flow0Fragment1),   switch1(flow0Fragment3), dev3,<br>   <br>                                                                                                                                                                                                                                                    |    <br>"flows": [ <br>   <br>…<br>   <br>"hops": [<br>   <br>{<br>   <br>            "nextNodeName": "switch1",<br>   <br>            "currentNodeName": "switch9",<br>   <br>            "priority": 7<br>   <br>        },<br>   <br> <br>   <br>        {<br>   <br>            "nextNodeName": "switch6",<br>   <br>            "currentNodeName": "switch1",<br>   <br>            "priority": 1<br>   <br>        },<br>   <br>        {<br>   <br>            "nextNodeName": "dev3",<br>   <br>            "currentNodeName": "switch6",<br>   <br>            "priority": 1<br>   <br>        },<br>   <br>…<br>   <br>]<br>   <br>---------<br>   <br>Flow List:<br>   <br>…<br>   <br>Path to dev3: dev29, switch9(flow0Fragment1),   switch1(flow0Fragment3), dev3,<br>   <br>                                                      |
|    <br>Transmission   windows consistency -   Cycle          |    <br>A packet does not overpass its cycle also Two packets can't be   transmitted at same time at same port    |    <br>...<br>   <br>"slotDuration":   0.576,<br>   <br>"slotStart":   499.424<br>   <br>...<br>   <br>"slotDuration":   0.576,<br>   <br>"slotStart":   489.568                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |    <br>...<br>   <br>"slotDuration":   1.576,<br>   <br> "slotStart": 499.424<br>   <br>...<br>   <br>"slotDuration":   0.576,<br>   <br>"slotStart":   499.424                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
|    <br>Transmission windows consistency – Priority Window    |    <br>Each packet must be transmitted at its correspondent priority window                                      |    <br>"flows": [ <br>   <br>…<br>   <br>"hops": [<br>   <br>        {<br>   <br>            "nextNodeName": "switch1",<br>   <br>            "currentNodeName": "switch9",<br>   <br>          "priority": 7<br>   <br>        },<br>   <br>…<br>   <br>]<br>   <br>------------<br>   <br> <br>   <br>"switches": [<br>   <br>    {<br>   <br>        "name": "switch9",<br>   <br>      “ports”:   [<br>   <br>        "prioritySlotsData": [<br>   <br>            {<br>   <br>                "slotsData": [<br>   <br>                  {<br>   <br>                    "slotDuration": 0.58,<br>   <br>                    "slotStart": 8.572<br>   <br>                  }<br>   <br>                ],<br>   <br>              "priority": 7<br>   <br>            }<br>   <br>          ]<br>   <br>    ]<br>   <br>…<br>   <br>}]    |    <br>"flows": [ <br>   <br>…<br>   <br>"hops": [<br>   <br>        {<br>   <br>            "nextNodeName": "switch1",<br>   <br>            "currentNodeName": "switch9",<br>   <br>          "priority": 7<br>   <br>        },<br>   <br>…<br>   <br>]<br>   <br>------------<br>   <br> <br>   <br>"switches": [<br>   <br>    {<br>   <br>        "name": "switch9",<br>   <br>      “ports”:   [<br>   <br>        "prioritySlotsData": [<br>   <br>            {<br>   <br>                "slotsData": [<br>   <br>                  {<br>   <br>                    "slotDuration": 0.58,<br>   <br>                    "slotStart": 8.572<br>   <br>                  }<br>   <br>                ],<br>   <br>              "priority": 6<br>   <br>            }<br>   <br>          ]<br>   <br>    ]<br>   <br>…<br>   <br>}]    |
|    <br>Transmission windows consistency – Sent Order         |    <br>Packets at the same port of same priority must be sent in arrival   order(FIFO like)                      |    <br>"prioritySlotsData": [<br>   <br>            {<br>   <br>                "slotsData": [<br>   <br>                  {<br>   <br>                    "slotDuration": 0.576,<br>   <br>                    "slotStart":   498.115<br>   <br>                  }<br>   <br>                ],<br>   <br>                "priority": 5<br>   <br>            },<br>   <br>            {<br>   <br>                "slotsData": [<br>   <br>                  {<br>   <br>                    "slotDuration": 0.577,<br>   <br>                    "slotStart":   498.843<br>   <br>                  }<br>   <br>                ],<br>   <br>                "priority": 5<br>   <br>            }<br>   <br>          ]                                                                                                                    |    <br>"prioritySlotsData": [<br>   <br>            {<br>   <br>                "slotsData": [<br>   <br>                  {<br>   <br>                    "slotDuration": 0.576,<br>   <br>                    "slotStart":   498.815<br>   <br>                  }<br>   <br>                ],<br>   <br>                "priority": 5<br>   <br>            },<br>   <br>            {<br>   <br>                "slotsData": [<br>   <br>                  {<br>   <br>                    "slotDuration": 0.577,<br>   <br>                    "slotStart":   498.143<br>   <br>                  }<br>   <br>                ],<br>   <br>                "priority": 5<br>   <br>            }<br>   <br>          ]                                                                                                                    |