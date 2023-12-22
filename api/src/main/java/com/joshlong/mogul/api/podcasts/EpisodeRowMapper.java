package com.joshlong.mogul.api.podcasts;

import com.joshlong.mogul.api.managedfiles.ManagedFile;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

class EpisodeRowMapper implements RowMapper<Episode> {

	private final Function<Long, Podcast> podcastFunction;

	private final Function<Long, ManagedFile> managedFileFunction;

	EpisodeRowMapper(Function<Long, Podcast> podcastFunction, Function<Long, ManagedFile> managedFileFunction) {
		this.podcastFunction = podcastFunction;
		this.managedFileFunction = managedFileFunction;
	}

	@Override
	public Episode mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new Episode(//
				rs.getLong("id"), //
				this.podcastFunction.apply(rs.getLong("podcast_id")), //
				rs.getString("title"), //
				rs.getString("description"), //
				rs.getDate("created"), //
				this.managedFileFunction.apply(rs.getLong("graphic")), //
				this.managedFileFunction.apply(rs.getLong("produced_graphic")), //
				this.managedFileFunction.apply(rs.getLong("introduction")), //
				this.managedFileFunction.apply(rs.getLong("produced_introduction")), //
				this.managedFileFunction.apply(rs.getLong("interview")), //
				this.managedFileFunction.apply(rs.getLong("produced_interview")), //
				this.managedFileFunction.apply(rs.getLong("produced_audio")), //
				rs.getBoolean("complete"), //
				rs.getDate("produced_audio_updated"), //
				rs.getDate("produced_audio_assets_updated")//
		);
	}

}
