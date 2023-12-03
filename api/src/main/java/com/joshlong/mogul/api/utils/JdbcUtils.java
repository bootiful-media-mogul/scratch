package com.joshlong.mogul.api.utils;

import java.net.URI;
import java.sql.ResultSet;

public abstract class JdbcUtils {

	/*
	 * URIs are not supported by the PostgreSQL JDBC driver, so we fake it 'till we make
	 * it. (sure would be nice to have extension functions though)
	 *
	 */
	public static URI uri(ResultSet resultSet, String columnName) {
		try {
			var string = resultSet.getString(columnName);
			if (string == null)
				return null;
			return new URI(string);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

}
