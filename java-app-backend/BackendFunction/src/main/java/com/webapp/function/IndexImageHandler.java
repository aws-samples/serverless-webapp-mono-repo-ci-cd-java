package com.webapp.function;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.IndexFacesRequest;
import software.amazon.awssdk.services.rekognition.model.IndexFacesResponse;
import software.amazon.awssdk.services.rekognition.model.S3Object;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import static software.amazon.lambda.powertools.tracing.TracingUtils.putAnnotation;
import static software.amazon.lambda.powertools.tracing.TracingUtils.putMetadata;

public class IndexImageHandler implements RequestHandler<S3EventNotification, IndexFacesResponse> {
    private static final Logger LOG = LogManager.getLogger(IndexImageHandler.class);

    private static final String COLLECTION_ID = System.getenv("CollectionId");
    private static final String TABLE_NAME = System.getenv("TableName");

    private static final RekognitionClient rekognitionClient = RekognitionClient.create();
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final S3Client s3Client = S3Client.create();

    @Override
    @Logging(logEvent = true, samplingRate = 0.5)
    @Tracing(namespace = "ImageIndexer")
    public IndexFacesResponse handleRequest(S3EventNotification input, Context context) {
        String bucketName = input.getRecords().get(0).getS3().getBucket().getName();
        String bucketKey = input.getRecords().get(0).getS3().getObject().getUrlDecodedKey();

        try {
            IndexFacesResponse indexFacesResponse = indexFaces(bucketName, bucketKey);
            LOG.debug("Response from service after indexing {}", indexFacesResponse);

            if (indexFacesResponse.sdkHttpResponse().isSuccessful()) {
                String faceId = indexFacesResponse.faceRecords().get(0).face().faceId();

                HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(bucketKey)
                        .build());

                String fullname = headObjectResponse.metadata().getOrDefault("fullname", "");

                putMetadata("full_name", fullname);
                putAnnotation("full_name", fullname);

                updateIndexDetails(faceId, fullname);
            }

            return indexFacesResponse;
        } catch (Exception e) {
            LOG.error("Failed indexing uploaded image from bucket {} with key {}", bucketName, bucketKey, e);
            throw e;
        }

    }

    @Tracing
    private IndexFacesResponse indexFaces(final String bucketName,
                                          final String bucketKey) {

        IndexFacesResponse indexFacesResponse = rekognitionClient.indexFaces(IndexFacesRequest.builder()
                .collectionId(COLLECTION_ID)
                .image(builder -> builder.s3Object(S3Object.builder()
                        .bucket(bucketName)
                        .name(bucketKey)
                        .build()))
                .build());

        LOG.debug("Response from index face: {}", indexFacesResponse);
        return indexFacesResponse;
    }

    @Tracing
    private void updateIndexDetails(final String faceId,
                                    final String fullname) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("RekognitionId", AttributeValue.builder()
                .s(faceId)
                .build());

        item.put("FullName", AttributeValue.builder()
                .s(fullname)
                .build());

        PutItemResponse putItemResponse = dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build());

        LOG.debug("Persistance response from dynamo db {}", putItemResponse);
    }
}
