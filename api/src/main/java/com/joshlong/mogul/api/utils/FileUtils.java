package com.joshlong.mogul.api.utils;

import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public abstract class FileUtils {

	public static File tempFile() {
		try {
			return Files.createTempFile("mogul-temp", "temp-file").toFile();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean delete(File file) {
		if (file != null && file.exists()) {
			if (file.isDirectory())
				FileSystemUtils.deleteRecursively(file);
			return file.delete();
		}
		return false;
	}

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
