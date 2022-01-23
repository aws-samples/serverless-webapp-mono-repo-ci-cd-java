package com.webapp.listfaces;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;


public class ListFacesHandler implements RequestHandler<Object, APIGatewayProxyResponseEvent> {

    private static final String TABLE_NAME = System.getenv("TableName");
    private static final String CF_DISTRIBUTION = System.getenv("CloudFrontDistribution");
    private static final String PLACEHOLDER_URL = "https://" + CF_DISTRIBUTION + "/index/static/aws_logo.png";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(Object input, Context context) {

        final LambdaLogger logger = context.getLogger();

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST,GET");
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        logger.log("Listing all Faces");
        //TODO Get the List of indexed faces from DynamoDB
        final List<FacePojo> list = new ArrayList<FacePojo>(Collections.nCopies(5, new FacePojo(PLACEHOLDER_URL, "Placeholder")));

        try {
            return apiGatewayProxyResponseEvent
                    .withStatusCode(200)
                    .withBody(OBJECT_MAPPER.writeValueAsString(list));
        } catch (JsonProcessingException e) {
            logger.log("Failed sending response" + e.toString());
            throw new RuntimeException("Something went wrong");
        }
    }

}


