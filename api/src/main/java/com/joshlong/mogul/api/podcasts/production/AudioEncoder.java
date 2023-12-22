package com.joshlong.mogul.api.podcasts.production;

import com.joshlong.mogul.api.utils.FileUtils;
import com.joshlong.mogul.api.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.InputStreamReader;

@Component
class AudioEncoder {

	private final Logger log = LoggerFactory.getLogger(getClass());

	AudioEncoder() throws Exception {
		try (var cmd = new ProcessBuilder().command("ffmpeg", "-version").start().getInputStream();
				var ow = new InputStreamReader(cmd)) {
			var cmdOutput = FileCopyUtils.copyToString(ow);
			log.debug(cmdOutput);
		}
	}

	File encode(File input) {
		try {
			var mp3Ext = "mp3";
			if (input.getAbsolutePath().endsWith(mp3Ext))
				return input;
			var mp3 = FileUtils.createRelativeTempFile(input, "." + mp3Ext);
			ProcessUtils.runCommand("ffmpeg", "-i " + input.getAbsolutePath(), "-o " + mp3.getAbsolutePath());
			return mp3;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
