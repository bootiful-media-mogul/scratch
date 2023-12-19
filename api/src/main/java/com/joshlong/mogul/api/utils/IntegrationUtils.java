package com.joshlong.mogul.api.utils;

import com.joshlong.mogul.api.old.PodcastIntegrations;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericHandler;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 *
 */
public abstract class IntegrationUtils {

	public static GenericHandler<Object> terminatingDebugHandler() {
		return terminatingDebugHandler(null);
	}

	public static GenericHandler<Object> terminatingDebugHandler(String header) {
		var log = LoggerFactory.getLogger(PodcastIntegrations.class);
		return (payload, headers) -> {
			var message = new StringBuilder();
			if (StringUtils.hasText(header))
				message.append(header.toUpperCase(Locale.ROOT));
			message.append(System.lineSeparator());
			message.append("---------------");
			message.append(System.lineSeparator());
			message.append(payload.toString());
			message.append(System.lineSeparator());
			headers
				.forEach((k, v) -> message.append("\t").append(k).append('=').append(v).append(System.lineSeparator()));
			message.append(System.lineSeparator());
			log.debug(message.toString());
			return null;
		};
	}

	public static GenericHandler<Object> debugHandler() {
		return debugHandler(null);
	}

	public static GenericHandler<Object> debugHandler(String header) {
		return (payload, headers) -> {
			terminatingDebugHandler(header).handle(payload, headers);
			return payload;
		};
	}

}
