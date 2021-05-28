package nesting.simulations.examples.nestSched;

import ned.DatarateChannel;
import nesting.node.ethernet.VlanEtherSwitchPreemptable;

network nestSched
{
	types:
		channel C extends DatarateChannel
		{
			delay = 0ns;
			datarate = 1Gbps;
		}

	submodules:
		dev0: NestSchedDev;
		dev1: NestSchedDev;
		dev2: NestSchedDev;
		dev3: NestSchedDev;
		dev4: NestSchedDev;



		switch0: VlanEtherSwitchPreemptable {
			parameters:
			gates:
				ethg[5];
		}



	connections:
		switch0.ethg[0] <--> C <--> dev0.ethg;
		switch0.ethg[1] <--> C <--> dev1.ethg;
		switch0.ethg[2] <--> C <--> dev2.ethg;
		switch0.ethg[3] <--> C <--> dev3.ethg;
		switch0.ethg[4] <--> C <--> dev4.ethg;


}
