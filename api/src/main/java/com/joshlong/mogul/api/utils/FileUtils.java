package com.joshlong.mogul.api.utils;

import org.springframework.util.Assert;

import java.io.File;
import java.util.UUID;

public abstract class FileUtils {

	public static File createRelativeTempFile(File in) {
		Assert.notNull(in, "the file must not be null");
		var path = in.getAbsolutePath();
		var ext = path.substring(path.lastIndexOf("."));
		return new File(in.getParentFile(), UUID.randomUUID() + ext);
	}

}
