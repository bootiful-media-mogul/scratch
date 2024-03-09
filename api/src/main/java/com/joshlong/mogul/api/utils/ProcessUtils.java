package com.joshlong.mogul.api.utils;

import java.io.IOException;

public abstract class ProcessUtils {

	public static int runCommand(String... command) throws IOException, InterruptedException {
		var processBuilder = new ProcessBuilder(command);
		var process = processBuilder.start();
		return process.waitFor();
	}

}
