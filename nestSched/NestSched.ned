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



		switch0: VlanEtherSwitchPreemptable {
			parameters:
			gates:
				ethg[3];
		}



		dev1: NestSchedDev;
		dev2: NestSchedDev;
		dev3: NestSchedDev;



		switch1: VlanEtherSwitchPreemptable {
			parameters:
			gates:
				ethg[4];
		}



		dev4: NestSchedDev;
		dev5: NestSchedDev;
		dev6: NestSchedDev;



		switch2: VlanEtherSwitchPreemptable {
			parameters:
			gates:
				ethg[4];
		}



	connections:
		switch0.ethg[0] <--> C <--> switch2.ethg[0];
		switch0.ethg[1] <--> C <--> switch1.ethg[0];
		switch0.ethg[2] <--> C <--> dev0.ethg;


		switch1.ethg[1] <--> C <--> dev1.ethg;
		switch1.ethg[2] <--> C <--> dev2.ethg;
		switch1.ethg[3] <--> C <--> dev3.ethg;


		switch2.ethg[1] <--> C <--> dev4.ethg;
		switch2.ethg[2] <--> C <--> dev5.ethg;
		switch2.ethg[3] <--> C <--> dev6.ethg;


}
