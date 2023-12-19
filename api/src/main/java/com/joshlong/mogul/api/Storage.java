package com.joshlong.mogul.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

@Component
public class Storage {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final S3Client s3;

	private static final DataSize LARGE_FILE = DataSize.ofMegabytes(50);

	public Storage(S3Client s3) {
		this.s3 = s3;
	}

	public void remove(URI uri) {
		validUri(uri);
		this.remove(uri.getHost(), uri.getPath());
	}

	public void remove(String bucket, String objectName) {
		if (bucketExists(bucket)) {
			var delete = DeleteObjectRequest.builder().bucket(bucket).key(objectName).build();
			s3.deleteObject(delete);
		}
	}

	public void write(URI uri, Resource resource) {
		validUri(uri);
		write(uri.getHost(), uri.getPath(), resource);
	}

	private void doWriteForLargeFiles(String bucketName, String keyName, Resource resource) {

		var contentLength = 0L;
		try {
			contentLength = resource.contentLength();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.debug("large file [" + contentLength + "]: " + resource.getFilename());
		var partSize = LARGE_FILE.toBytes();

		try {
			// Create a multipart upload request
			var createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
				.bucket(bucketName)
				.key(keyName)
				.build();
			var response = s3.createMultipartUpload(createMultipartUploadRequest);
			var uploadId = response.uploadId();
			log.info("uploading with upload ID " + uploadId);
			// Divide the file into parts and upload each part
			var completedParts = new ArrayList<CompletedPart>();
			for (var i = 0L; i < contentLength; i += partSize) {
				// Create a request to upload a part
				var partNumber = (int) (i / partSize + 1);
				var uploadRequest = UploadPartRequest.builder()
					.bucket(bucketName)
					.key(keyName)
					.uploadId(uploadId)
					.partNumber(partNumber)
					.build();
				var etag = s3
					.uploadPart(uploadRequest,
							RequestBody.fromInputStream(resource.getInputStream(), resource.contentLength()))
					.eTag();
				completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(etag).build());
				log.debug("uploaded part #" + partNumber + " for resource [" + resource.getFilename() + "]");
			}
			var completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
				.bucket(bucketName)
				.key(keyName)
				.uploadId(uploadId)
				.multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
				.build();
			s3.completeMultipartUpload(completeMultipartUploadRequest);

			log.debug("finished uploading a file [" + resource.getFilename() + "] with content length "
					+ resource.contentLength());

		} //
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private void doWriteForSmallFiles(String bucket, String objectName, Resource resource) {
		try (var inputStream = new BufferedInputStream(resource.getInputStream())) {
			var putOb = PutObjectRequest.builder().bucket(bucket).key(objectName).metadata(Map.of()).build();

			s3.putObject(putOb, RequestBody.fromInputStream(inputStream, resource.contentLength()));
		}
		catch (IOException e) {
			var errMsg = "got an exception writing to [" + bucket + "] for file [" + objectName + "]";
			log.error(errMsg);
			throw new RuntimeException(errMsg);
		}
		log.debug("finished executing an S3 PUT for [" + bucket + '/' + objectName + "] on thread ["
				+ Thread.currentThread() + "]");
	}

	public void write(String bucket, String objectName, Resource resource) {

		log.debug("started executing an S3 PUT for [" + bucket + '/' + objectName + "] on thread ["
				+ Thread.currentThread() + "]");

		ensureBucketExists(bucket);
		var len = -1L;
		try {
			len = resource.contentLength();
		}
		catch (Throwable throwable) {
			// ...
		}
		if (len < LARGE_FILE.toBytes()) {
			doWriteForSmallFiles(bucket, objectName, resource);
		} //
		else {
			doWriteForLargeFiles(bucket, objectName, resource);
		}

	}

	private boolean bucketExists(String bucketName) {
		var buckets = (this.s3.listBuckets());
		if (buckets.hasBuckets()) {
			return buckets.buckets().stream().anyMatch(bucket -> bucket.name().equalsIgnoreCase(bucketName));
		}
		return false;
	}

	private void ensureBucketExists(String bucketName) {
		try {
			if (!bucketExists(bucketName)) {
				log.info("attempting to create the bucket called [" + bucketName + "]");
				s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
			}
			else {
				log.debug("the bucket named [" + bucketName + "] already exists");
			}
		} //
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	public Resource read(String bucket, String objectName) {
		var getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(objectName).build();
		return new InputStreamResource(new BufferedInputStream(s3.getObject(getObjectRequest)));
	}

	public Resource read(URI uri) {
		validUri(uri);
		return this.read(uri.getHost(), uri.getPath());
	}

	private static void validUri(URI uri) {
		Assert.state(
				uri != null && uri.getScheme().toLowerCase().equalsIgnoreCase("s3")
						&& uri.getPath().split("/").length == 2,
				"this uri [" + Objects.requireNonNull(uri) + "] is not a valid s3 reference");
	}

}
