package com.joshlong.mogul.api.utils;

import org.springframework.util.StringUtils;

import java.net.URI;

public abstract class UriUtils {

	public static URI nullSafeUri(String s) {
		if (StringUtils.hasText(s))
			return URI.create(s);
		return null;
	}

}
