package com.joshlong.mogul.api.managedfiles;

import com.joshlong.mogul.api.ManagedFileService;
import com.joshlong.mogul.api.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;

@Service
@Transactional
class DefaultManagedFileService implements ManagedFileService {

	private final JdbcClient db;

	private final Storage storage;

	private final Logger log = LoggerFactory.getLogger(getClass());

	DefaultManagedFileService(JdbcClient db, Storage storage) {
		this.db = db;
		this.storage = storage;
	}

	@Override
	public ManagedFile getManagedFile(Long managedFileId) {
		if (null == managedFileId || managedFileId == 0)
			return null;
		return this.db.sql("select * from managed_file where id =? ")
			.param(managedFileId)
			.query(new ManagedFileRowMapper())
			.single();
	}

	@Override
	public Resource read(Long managedFileId) {
		var mf = getManagedFile(managedFileId);
		return this.storage.read(mf.bucket(), mf.folder() + '/' + mf.filename());
	}

	private long contentLength(Resource resource) {
		try {
			return resource.contentLength();
		}//
		catch (Throwable throwable) {
			return 0;
		}
	}

	@Override
	public ManagedFile write(Long managedFileId, Resource resource) {
		var mf = getManagedFile(managedFileId);
		var bucket = mf.bucket();
		var folder = mf.folder();
		var fn = mf.filename();
		this.storage.write(bucket, folder + '/' + fn, resource);
		this.db.sql("update managed_file set written = true , size =? where id=?")
				.params(contentLength(resource), managedFileId)
				.update();
		return getManagedFile(managedFileId);
	}

	@Override
	public ManagedFile createManagedFile(Long mogulId, String bucket, String folder, String fileName, long size) {
		var kh = new GeneratedKeyHolder();
		this.db.sql("insert into managed_file(mogul_id,   bucket, folder, filename, size ) VALUES ( ?, ?, ?, ?, ? )")
			.params(mogulId, bucket, folder, fileName, size)
			.update(kh);
		log.info("the bucket is [" + bucket + "]");
		return getManagedFile(((Number) kh.getKeys().get("id")).longValue());
	}

}

class ManagedFileRowMapper implements RowMapper<ManagedFile> {

	@Override
	public ManagedFile mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new ManagedFile(rs.getLong("mogul_id"), rs.getLong("id"), rs.getString("bucket"), rs.getString("folder"),
				rs.getString("filename"), rs.getDate("created"),
				rs.getBoolean("written") ,
				rs.getLong("size"));
	}

}