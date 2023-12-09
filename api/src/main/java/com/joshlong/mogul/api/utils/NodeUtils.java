package com.joshlong.mogul.api.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public abstract class NodeUtils {

	public static String nodeId() {
		try {
			return InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}

	}

}
