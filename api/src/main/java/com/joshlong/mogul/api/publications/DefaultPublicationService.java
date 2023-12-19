package com.joshlong.mogul.api.publications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.mogul.api.MogulService;
import com.joshlong.mogul.api.Settings;
import com.joshlong.mogul.api.utils.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
class DefaultPublicationService implements PublicationService {

	private final Map<String, PublisherPlugin<?>> plugins = new ConcurrentHashMap<>();

	private final JdbcClient db;

	private final Settings settings;

	private final MogulService mogulService;

	private final ObjectMapper om;

	private final RowMapper<Publication> publicationRowMapper;

	private final TextEncryptor textEncryptor;

	DefaultPublicationService(JdbcClient db, Settings settings, MogulService mogulService, TextEncryptor textEncryptor,
			Map<String, PublisherPlugin<?>> plugins, ObjectMapper om) {
		this.db = db;
		this.settings = settings;
		this.mogulService = mogulService;
		this.textEncryptor = textEncryptor;
		this.plugins.putAll(plugins);
		this.om = om;
		Assert.notNull(this.db, "the JdbcClient must not be null");
		Assert.notNull(this.mogulService, "the mogulService must not be null");
		Assert.notNull(this.textEncryptor, "the textEncryptor must not be null");
		Assert.notNull(this.settings, "the settings must not be null");
		Assert.state(!this.plugins.isEmpty(), "there are no plugins for publication");
		this.publicationRowMapper = new PublicationRowMapper(om, textEncryptor);
	}

	private String json(Object o) {
		try {
			return this.om.writeValueAsString(o);
		} //
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <T extends Publishable> Publication publish(Long mogulId, T payload, Map<String, String> contextAndSettings,
			PublisherPlugin<T> plugin) {
		var mogul = mogulService.getMogulById(mogulId);
		Assert.notNull(plugin, "the plugin must not be null");
		Assert.notNull(payload, "the payload must not be null");
		Assert.notNull(mogul, "the mogul should not be null");
		var configuration = this.settings.getAllValuesByCategory(this.mogulService.getCurrentMogul().id(),
				plugin.name());
		var context = new HashMap<String, String>();
		context.putAll(configuration);
		context.putAll(contextAndSettings);
		plugin.publish(context, payload);
		var contextJson = textEncryptor.encrypt(json(context));
		var publicationData = textEncryptor.encrypt(json(payload.publicationKey()));
		var entityClazz = payload.getClass().getName();
		var kh = new GeneratedKeyHolder();
		this.db.sql(
				"insert into publication(mogul_id, plugin, created, published, context, payload , payload_class) VALUES (?,?,?,?,?,?,?)")
			.params(mogulId, plugin.name(), new Date(), null, contextJson, publicationData, entityClazz)
			.update(kh);
		return this.getPublicationById(JdbcUtils.getIdFromKeyHolder(kh).longValue());
	}

	@Override
	public Publication getPublicationById(Long publicationId) {
		return db.sql("select * from publication where id =? ")
			.params(publicationId)
			.query(this.publicationRowMapper)
			.single();
	}

}

class PublicationRowMapper implements RowMapper<Publication> {

	private final ObjectMapper objectMapper;

	private final TextEncryptor textEncryptor;

	private final Logger log = LoggerFactory.getLogger(getClass());

	PublicationRowMapper(ObjectMapper objectMapper, TextEncryptor textEncryptor) {
		this.objectMapper = objectMapper;
		this.textEncryptor = textEncryptor;
	}

	private Class<?> classFor(String name) {
		try {
			Assert.hasText(name, "you must provide a non-empty class name");
			return Class.forName(name);
		}
		catch (ClassNotFoundException e) {
			log.warn("classNotFoundException when trying to do Class.forName(" + name + ") ", e);
		}
		return null;
	}

	@Override
	public Publication mapRow(ResultSet rs, int rowNum) throws SQLException {
		var context = readContextFor(rs.getString("context"));
		var payload = this.textEncryptor.decrypt(rs.getString("payload"));
		return new Publication(rs.getLong("mogul_id"), rs.getLong("id"), rs.getString("plugin"), rs.getDate("created"),
				rs.getDate("published"), context, payload, classFor(rs.getString("payload_class")));
	}

	private Map<String, String> readContextFor(String context) {
		var decrypted = this.textEncryptor.decrypt(context);
		// @formatter:off
		var parameterizedTypeReference = new TypeReference<Map<String, String>>() {};
		// @formatter:on
		try {
			return this.objectMapper.readValue(decrypted, parameterizedTypeReference);
		} //
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
