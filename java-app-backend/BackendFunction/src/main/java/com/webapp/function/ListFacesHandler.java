package com.webapp.function;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;

import java.util.*;

import static software.amazon.lambda.powertools.tracing.TracingUtils.putAnnotation;
import static software.amazon.lambda.powertools.tracing.TracingUtils.putMetadata;

public class ListFacesHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOG = LogManager.getLogger(ListFacesHandler.class);

    private static final String TABLE_NAME = System.getenv("TableName");
    private static final String CF_DISTRIBUTION = System.getenv("CloudFrontDistribution");
    private static final String PLACEHOLDER_URL = "https://" + CF_DISTRIBUTION + "/index/static/aws_logo.png" ;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    @Override
    @Logging(logEvent = true, samplingRate = 0.5)
    @Tracing(namespace = "ListFaces")
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST,GET");
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        LOG.debug("Listing all Faces");
        final List<FacePojo> list = new ArrayList<FacePojo>(Collections.nCopies(5, new FacePojo(PLACEHOLDER_URL, "Placeholder")));

        try {
            return apiGatewayProxyResponseEvent
                    .withStatusCode(200)
                    .withBody(OBJECT_MAPPER.writeValueAsString(list));
        } catch (JsonProcessingException e) {
            LOG.error("Failed sending response", e);
            throw new RuntimeException(e);
        }
    }

}
