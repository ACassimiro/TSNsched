package com.tsnsched.core.interface_manager;

import com.tsnsched.core.network.Network;

public interface GenericParser {

	public Network parseInput();
	public Network parseInputContent(String content);
	public void setPrinter(Printer printer);
}
