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
import org.springframework.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

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
	public ManagedFileDeletionRequest getManagedFileDeletionRequest(Long managedFileDeletionRequestId) {
		return db.sql("select * from managed_file_deletion_request where id =? ").param(managedFileDeletionRequestId).query(new ManagedFileDeletionRequestRowMapper()).single();
	}

	@Override
	public Collection<ManagedFileDeletionRequest> getOutstandingManagedFileDeletionRequests() {
		return this.db.sql("select * from managed_file_deletion_request where deleted = false").query(new ManagedFileDeletionRequestRowMapper()).list();
	}

	@Override
	public void complete(Long managedFileDeletionRequestId) {
		var mfRequest = getManagedFileDeletionRequest(managedFileDeletionRequestId);
		storage.remove(mfRequest.bucket(), mfRequest.folder() + '/' + mfRequest.filename());
		Assert.notNull(mfRequest, "the managed file deletion request should not be null");
		this.db.sql(" update  managed_file_deletion_request  set deleted = true where id = ? ")
				.param(managedFileDeletionRequestId)
				.update();
		var mfdr = getManagedFileDeletionRequest(managedFileDeletionRequestId);
		log.debug("completed [" + mfdr + "]");
	}

	@Override
	public void deleteManagedFile(Long managedFileId) {
		var mf = getManagedFile(managedFileId);
		db.sql("delete from managed_file where id =?").param(managedFileId).update();
		db.sql("insert into managed_file_deletion_request ( mogul_id, bucket, folder, filename) values(?,? ,?,?)")
				.params(mf.mogulId(), mf.bucket(), mf.folder(), mf.filename())
				.update();
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
	public void write(Long managedFileId, String filename, Resource resource) {
		var managedFile = getManagedFile(managedFileId);
		var bucket = managedFile.bucket();
		var folder = managedFile.folder();
		this.storage
				.write(bucket, folder + '/' + filename, resource);
		this.db
				.sql("update managed_file set filename =?, written = true , size =? where id=?")
				.params(filename, contentLength(resource), managedFileId)
				.update();
		var wroteMf = getManagedFile(managedFileId);
		log.info("managed file has been written? "  + wroteMf.written());
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

class ManagedFileDeletionRequestRowMapper implements RowMapper<ManagedFileDeletionRequest> {

	@Override
	public ManagedFileDeletionRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new ManagedFileDeletionRequest(
				rs.getLong("id"),
				rs.getLong("mogul_id"),
				rs.getString("bucket"),
				rs.getString("folder"),
				rs.getString("filename"),
				rs.getBoolean("deleted"),
				rs.getDate("created")
		);
	}
}
class ManagedFileRowMapper implements RowMapper<ManagedFile> {

	@Override
	public ManagedFile mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new ManagedFile(rs.getLong("mogul_id"), rs.getLong("id"),
				rs.getString("bucket"), rs.getString("folder"),
				rs.getString("filename"), rs.getDate("created"),
				rs.getBoolean("written") ,
				rs.getLong("size"));
	}

}