package com.joshlong.mogul.api.publications;

import com.joshlong.mogul.api.Mogul;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Publication {

	private final Mogul mogul;

	private final Date created;

	private final Date published;

	private final String payload;

	private final String plugin;

	private final Map<String, String> context;

	Publication(Mogul mogul, String plugin, Date created, Date published, Map<String, String> context, String payload) {
		this.mogul = mogul;
		this.context = context;
		this.plugin = plugin;
		this.created = created;
		this.published = published;
		this.payload = payload;
	}

	Publication(Mogul mogul, String plugin, Date created, String payload) {
		this(mogul, plugin, created, null, new HashMap<>(), payload);
	}

	Publication(Mogul mogul, String plugin, Date created, Map<String, String> context, String payload) {
		this(mogul, plugin, created, null, context, payload);
	}

	public Mogul mogul() {
		return this.mogul;
	}

	public Date created() {
		return this.created;
	}

	public Date published() {
		return this.published;
	}

	public String payload() {
		return this.payload;
	}

	public Map<String, String> context() {
		return this.context;
	}

	public String plugin() {
		return plugin;
	}

	public static Publication of(Mogul mogul, String plugin, Map<String, String> context, String payload) {
		return new Publication(mogul, plugin, new Date(), context, payload);
	}

}
