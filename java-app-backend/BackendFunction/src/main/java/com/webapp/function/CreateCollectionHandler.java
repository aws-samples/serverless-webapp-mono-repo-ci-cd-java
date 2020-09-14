package com.webapp.function;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.CreateCollectionResponse;
import software.amazon.awssdk.services.rekognition.model.DeleteCollectionRequest;
import software.amazon.awssdk.services.rekognition.model.DeleteCollectionResponse;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;

/**
 * Handler for requests to Lambda function.
 */
public class CreateCollectionHandler implements RequestHandler<Map<String, Object>, Object> {
    private static final Logger LOG = LogManager.getLogger(CreateCollectionHandler.class);
    private static final RekognitionClient client = RekognitionClient.create();

    @PowertoolsLogging(logEvent = true)
    public APIGatewayProxyResponseEvent handleRequest(final Map<String, Object> input, final Context context) {
        String collectionId = (String) ((Map) input.get("ResourceProperties")).get("CollectionName");

        if ("Delete".equals(input.get("RequestType"))) {
            callGracefully(input, context, () -> {

                DeleteCollectionResponse deleteCollectionResponse = client.deleteCollection(DeleteCollectionRequest.builder()
                        .collectionId(collectionId)
                        .build());

                if (deleteCollectionResponse.statusCode() == 200) {
                    LOG.info("Resource deleted");
                    sendResponse(input, context, "SUCCESS");
                }
            });
        } else {
            callGracefully(input, context, () -> {
                CreateCollectionResponse response = client.createCollection(CreateCollectionRequest.builder()
                        .collectionId(collectionId)
                        .build());

                if (response.statusCode() == 200) {
                    LOG.info("Resource created");
                    sendResponse(input, context, "SUCCESS");
                }
            });
        }

        return null;
    }

    private void callGracefully(final Map<String, Object> input,
                                final Context context,
                                final Runnable collectionConsumer) {
        try {
            collectionConsumer.run();
        } catch (Exception e) {
            LOG.error("Failed executing reckognition client", e);
            sendResponse(input, context, "FAILED");
        }
    }

    private void sendResponse(
            final Map<String, Object> input,
            final Context context,
            final String responseStatus) {

        String responseUrl = (String) input.get("ResponseURL");
        LOG.info("ResponseURL: {}", responseUrl);

        try {
            URL url = new URL(responseUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");

            JSONObject responseBody = new JSONObject();
            responseBody.put("Status", responseStatus);
            responseBody.put("PhysicalResourceId", context.getLogStreamName());
            responseBody.put("StackId", input.get("StackId"));
            responseBody.put("RequestId", input.get("RequestId"));
            responseBody.put("LogicalResourceId", input.get("LogicalResourceId"));
            responseBody.put("Data", new JSONObject());

            OutputStreamWriter response = new OutputStreamWriter(connection.getOutputStream());
            response.write(responseBody.toString());
            response.close();
            LOG.info("Response Code: {}", connection.getResponseCode());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
