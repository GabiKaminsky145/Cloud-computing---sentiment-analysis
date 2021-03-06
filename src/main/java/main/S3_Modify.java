package main;

/*
 * Copyright 2011-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

// snippet-start:[s3.java2.s3_object_operations.complete]
// snippet-start:[s3.java2.s3_object_operations.import]

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class S3_Modify {

    private static S3Client s3;

    public static void createBucket(String bucket) {

        Region region = Region.US_EAST_1;
        s3 = S3Client.builder().region(region).build();
        ListBucketsResponse bucketsResponse = s3.listBuckets();
        List<Bucket> buckets = bucketsResponse.buckets();
        if (buckets.size() == 0) {
            s3.createBucket(CreateBucketRequest
                    .builder()
                    .bucket(bucket)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .build())
                    .build());
        }
    }

    public static void deleteBucket(String bucket_name) {
        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucket_name).build();
        s3.deleteBucket(deleteBucketRequest);
    }

    /**
     * Uploading an object to S3 in parts
     */
    public static void multipartUpload(String bucket_name, String key) throws IOException {

        int mb = 1024 * 1024;
        // First create a multipart upload and get upload id
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket_name).key(key)
                .build();
        CreateMultipartUploadResponse response = s3.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = response.uploadId();
        System.out.println(uploadId);

        // Upload all the different parts of the object
        UploadPartRequest uploadPartRequest1 = UploadPartRequest.builder().bucket(bucket_name).key(key)
                .uploadId(uploadId)
                .partNumber(1).build();
        String etag1 = s3.uploadPart(uploadPartRequest1, RequestBody.fromByteBuffer(getRandomByteBuffer(5 * mb))).eTag();
        CompletedPart part1 = CompletedPart.builder().partNumber(1).eTag(etag1).build();

        UploadPartRequest uploadPartRequest2 = UploadPartRequest.builder().bucket(bucket_name).key(key)
                .uploadId(uploadId)
                .partNumber(2).build();
        String etag2 = s3.uploadPart(uploadPartRequest2, RequestBody.fromByteBuffer(getRandomByteBuffer(3 * mb))).eTag();
        CompletedPart part2 = CompletedPart.builder().partNumber(2).eTag(etag2).build();


        // Finally call completeMultipartUpload operation to tell S3 to merge all uploaded
        // parts and finish the multipart operation.
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(part1, part2).build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                CompleteMultipartUploadRequest.builder().bucket(bucket_name).key(key).uploadId(uploadId)
                        .multipartUpload(completedMultipartUpload).build();
        s3.completeMultipartUpload(completeMultipartUploadRequest);
    }

    public static String location(String obj) {
        String ret = "Not found";
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
        List<com.amazonaws.services.s3.model.Bucket> bucketsResponse = s3.listBuckets();
        for (com.amazonaws.services.s3.model.Bucket bucket : bucketsResponse) {
            for (S3ObjectSummary summary : S3Objects.inBucket(s3, bucket.getName())) {
                if(summary.getKey().equals(obj))
                    ret = bucket.getName();
            }
        }
        return ret;
    }

    public static com.amazonaws.services.s3.model.S3Object getFile( String bucketName, String key){
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();

        return s3Client.getObject(new com.amazonaws.services.s3.model.GetObjectRequest(bucketName,key));
    }

    public static void getObject(String bucket, String key, String output) throws IOException {
        InstanceProfileCredentialsProvider provider = InstanceProfileCredentialsProvider.builder().build();
        s3 = (S3Client)((S3ClientBuilder)((S3ClientBuilder)S3Client.builder().region(Region.US_EAST_1)).credentialsProvider(provider)).build();
        s3.getObject((GetObjectRequest)GetObjectRequest.builder().bucket(bucket).key(key).build(), ResponseTransformer.toFile(Paths.get(output)));
    }

    private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
