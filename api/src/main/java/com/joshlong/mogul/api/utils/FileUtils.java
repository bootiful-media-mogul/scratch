package com.joshlong.mogul.api.utils;

import org.springframework.util.Assert;

import java.io.File;
import java.util.UUID;

public abstract class FileUtils {

	@SuppressWarnings("unused")
	public static File ensureDirectoryExists(File file) {
		Assert.state(file.exists() || file.mkdirs(), "the directory [" + file.getAbsolutePath() + "] does not exist");
		return file;
	}

	public static File createRelativeTempFile(File in, String ext) {
		Assert.notNull(in, "the file must not be null");
		return new File(in.getParentFile(), UUID.randomUUID() + ext);
	}

	public static File createRelativeTempFile(File in) {
		var path = in.getAbsolutePath();
		var ext = path.substring(path.lastIndexOf("."));
		return createRelativeTempFile(in, ext);
	}

}
