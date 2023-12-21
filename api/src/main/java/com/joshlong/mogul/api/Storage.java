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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Component
public class Storage {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final S3Client s3;


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

	/*
	 * writes 5mb chunks at a time to s3
	 */
	private void doWriteForLargeFiles(String bucketName, String keyName, Resource resource, DataSize maxSize) throws Exception {
		try (var inputStream = resource.getInputStream();) {
			var chunkSize = (int) maxSize.toBytes();
			var createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
				.bucket(bucketName)
				.key(keyName)
				.build();
			var response = s3.createMultipartUpload(createMultipartUploadRequest);
			var uploadId = response.uploadId();
			var completedParts = new ArrayList<CompletedPart>();
			var partNumber = 1;
			var buffer = new byte[chunkSize];
			var bytesRead = -1;
			while ((bytesRead = inputStream.read(buffer)) > 0) {
				var actualBytes = bytesRead == chunkSize ? buffer : Arrays.copyOf(buffer, bytesRead);
				var uploadPartRequest = UploadPartRequest.builder()
					.bucket(bucketName)
					.key(keyName)
					.uploadId(uploadId)
					.partNumber(partNumber)
					.build();
				var etag = s3.uploadPart(uploadPartRequest, RequestBody.fromBytes(actualBytes)).eTag();
				completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(etag).build());
				partNumber++;
				if (actualBytes != buffer) {
					buffer = new byte[chunkSize];
				}
			}
			var completedMultipartUpload = CompletedMultipartUpload.builder()
					.parts(completedParts)
					.build();
			var completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
				.bucket(bucketName)
				.key(keyName)
				.uploadId(uploadId)
					.multipartUpload(completedMultipartUpload)
				.build();

			s3.completeMultipartUpload(completeMultipartUploadRequest);
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

		var largeFile = DataSize.ofMegabytes(10);
		try {
			log.debug("started executing an S3 PUT for [" + bucket + '/' + objectName + "] on thread ["
					+ Thread.currentThread() + "]");

			ensureBucketExists(bucket);
		/*	var len = -1L;
			try {
				len = resource.contentLength();
			} catch (Throwable throwable) {
				// ...
			}
			if (len < largeFile.toBytes()) {
				doWriteForSmallFiles(bucket, objectName, resource);
			} //
			else {*/
			doWriteForLargeFiles(bucket, objectName, resource, largeFile);
		//	}
		}//
		catch (Throwable throwable) {
			throw new RuntimeException(throwable);
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
