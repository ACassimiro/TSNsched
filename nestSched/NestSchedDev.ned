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

	@signal[criticalFlowSig5](type=inet::Packet);
	@statistic[criticalFlowSig5Received](title="criticalFlowSig5Received"; source="dataAge(criticalFlowSig5)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig6](type=inet::Packet);
	@statistic[criticalFlowSig6Received](title="criticalFlowSig6Received"; source="dataAge(criticalFlowSig6)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig7](type=inet::Packet);
	@statistic[criticalFlowSig7Received](title="criticalFlowSig7Received"; source="dataAge(criticalFlowSig7)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig8](type=inet::Packet);
	@statistic[criticalFlowSig8Received](title="criticalFlowSig8Received"; source="dataAge(criticalFlowSig8)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig9](type=inet::Packet);
	@statistic[criticalFlowSig9Received](title="criticalFlowSig9Received"; source="dataAge(criticalFlowSig9)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig10](type=inet::Packet);
	@statistic[criticalFlowSig10Received](title="criticalFlowSig10Received"; source="dataAge(criticalFlowSig10)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig11](type=inet::Packet);
	@statistic[criticalFlowSig11Received](title="criticalFlowSig11Received"; source="dataAge(criticalFlowSig11)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig12](type=inet::Packet);
	@statistic[criticalFlowSig12Received](title="criticalFlowSig12Received"; source="dataAge(criticalFlowSig12)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig13](type=inet::Packet);
	@statistic[criticalFlowSig13Received](title="criticalFlowSig13Received"; source="dataAge(criticalFlowSig13)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig14](type=inet::Packet);
	@statistic[criticalFlowSig14Received](title="criticalFlowSig14Received"; source="dataAge(criticalFlowSig14)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig15](type=inet::Packet);
	@statistic[criticalFlowSig15Received](title="criticalFlowSig15Received"; source="dataAge(criticalFlowSig15)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig16](type=inet::Packet);
	@statistic[criticalFlowSig16Received](title="criticalFlowSig16Received"; source="dataAge(criticalFlowSig16)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig17](type=inet::Packet);
	@statistic[criticalFlowSig17Received](title="criticalFlowSig17Received"; source="dataAge(criticalFlowSig17)"; unit=s; record=histogram,vector; interpolationmode=linear);

	@signal[criticalFlowSig18](type=inet::Packet);
	@statistic[criticalFlowSig18Received](title="criticalFlowSig18Received"; source="dataAge(criticalFlowSig18)"; unit=s; record=histogram,vector; interpolationmode=linear);

}
