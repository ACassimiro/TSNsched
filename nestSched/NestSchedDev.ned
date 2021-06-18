package nesting.simulations.examples.nestSched;

import nesting.node.ethernet.VlanEtherHostSched;

module NestSchedDev extends VlanEtherHostSched {
	@signal[criticalFlowSig1](type=inet::Packet);
	@statistic[criticalFlowSig1Received](title="criticalFlowSig1Received"; source="dataAge(criticalFlowSig1)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig2](type=inet::Packet);
	@statistic[criticalFlowSig2Received](title="criticalFlowSig2Received"; source="dataAge(criticalFlowSig2)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig3](type=inet::Packet);
	@statistic[criticalFlowSig3Received](title="criticalFlowSig3Received"; source="dataAge(criticalFlowSig3)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig4](type=inet::Packet);
	@statistic[criticalFlowSig4Received](title="criticalFlowSig4Received"; source="dataAge(criticalFlowSig4)"; unit=s; record=histogram,vector; interpolationmode=linear);

}
