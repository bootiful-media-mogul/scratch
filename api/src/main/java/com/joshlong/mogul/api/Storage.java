package com.joshlong.mogul.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Map;

@Component
public class Storage {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final S3Client s3;

	public Storage(S3Client s3) {
		this.s3 = s3;
	}

	public void remove(String bucket, String objectName) {
		if (bucketExists(bucket)) {
			var delete = DeleteObjectRequest.builder().bucket(bucket).key(objectName).build();
			s3.deleteObject(delete);
		}
	}

	public void write(String bucket, String objectName, Resource resource) {

		log.info("started executing an S3 PUT for [" + bucket + '/' + objectName + "] on thread ["
				+ Thread.currentThread() + "]");

		ensureBucketExists(bucket);

		try (var inputStream = new BufferedInputStream(resource.getInputStream())) {
			var putOb = PutObjectRequest.builder().bucket(bucket).key(objectName).metadata(Map.of()).build();
			s3.putObject(putOb, RequestBody.fromInputStream(inputStream, resource.contentLength()));
		}
		catch (IOException e) {
			throw new RuntimeException("got an exception writing to [" + bucket + "] for file [" + objectName + "]");
		}
		log.info("finished executing an S3 PUT for [" + bucket + '/' + objectName + "] on thread ["
				+ Thread.currentThread() + "]");

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

}
