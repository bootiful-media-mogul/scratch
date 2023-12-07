package com.joshlong.mogul.api;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Settings {

	private final JdbcClient db;

	private final TextEncryptor encryptor;

	private final SettingsRowMapper rowMapper;

	Settings(JdbcClient db, TextEncryptor encryptor) {
		this.db = db;
		this.encryptor = encryptor;
		Assert.notNull(this.encryptor, "the encryptor must be non-null");
		Assert.notNull(this.db, "the db must be non-null");
		this.rowMapper = new SettingsRowMapper(encryptor);
	}

	private record Setting(String category, String key, String value) {
	}

	private static class SettingsRowMapper implements RowMapper<Setting> {

		private final TextEncryptor encryptor;

		SettingsRowMapper(TextEncryptor encryptor) {
			this.encryptor = encryptor;
			Assert.notNull(this.encryptor, "the " + TextEncryptor.class.getName() + " must be non-null");
		}

		@Override
		public Setting mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new Setting(rs.getString("category"), rs.getString("key"), encryptor.decrypt(rs.getString("value")));
		}

	}

	private Setting get(Long mogulId, String category, String key) {
		return this.db//
			.sql("select * from settings where mogul_id = ? and category =? and key = ? ")
			.params(mogulId, category, key)
			.query(this.rowMapper)
			.single();
	}

	public String getValue(Long mogulId, String category, String key) {
		return get(mogulId, category, key).value();
	}

	public String getString(Long mogulId, String category, String key) {
		return getValue(mogulId, category, key);
	}

	public int getInt(Long mogulId, String cat, String key) {
		return Integer.parseInt(getValue(mogulId, cat, key));
	}

	public double getDouble(Long moguleId, String category, String key) {
		return Double.parseDouble(getValue(moguleId, category, key));
	}

	public void set(Long mogulId, String category, String key, String value) {
		var updated = this.db.sql("""
				insert into settings(mogul_id, category, key, value)
				values (? ,? ,? ,? )
				on conflict on constraint  settings_mogul_id_category_key_key do update set value = excluded.value
				    """).params(mogulId, category, key, this.encryptor.encrypt(value)).update();

	}

}