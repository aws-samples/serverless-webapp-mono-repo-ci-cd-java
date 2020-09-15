package com.webapp.function;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.lambda.powertools.logging.PowertoolsLogging;
import software.amazon.lambda.powertools.tracing.PowertoolsTracing;

public class ImageUploadHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger LOG = LogManager.getLogger(ImageUploadHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final S3Presigner client = S3Presigner.create();

    private static final String S3_BUCKET = System.getenv("UploadBucket");

    @PowertoolsLogging(logEvent = true, samplingRate = 0.5)
    @PowertoolsTracing
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        apiGatewayProxyResponseEvent.withHeaders(headers);

        String contentType = input.getQueryStringParameters().getOrDefault("content-type", "");
        String fileExtension = input.getQueryStringParameters().getOrDefault("file-extension", "");

        if (contentType.isEmpty() || fileExtension.isEmpty()) {
            return apiGatewayProxyResponseEvent
                    .withStatusCode(400)
                    .withBody("{\n" +
                            "  \"message\": \"Both content-type and file-extension need to passed as query param!\" \n" +
                            "}");
        }

        String fileName = UUID.randomUUID() + fileExtension;
        String imagePath = "index/static/" + fileName;
        String personName = input.getQueryStringParameters().getOrDefault("person-name", "");
        LOG.debug("File path to be saved {} in bucket {}", imagePath, S3_BUCKET);
        LOG.debug("Received metadata {}", personName);

        HashMap<String, String> metaData = new HashMap<>();
        metaData.put("fullname", personName);

        PresignedPutObjectRequest requestObject = client.presignPutObject(req ->
                req.putObjectRequest(obj ->
                        obj.metadata(metaData)
                                .bucket(S3_BUCKET)
                                .key(imagePath)
                                .contentType(contentType)).signatureDuration(Duration.ofSeconds(60)));

        LOG.debug("Generated pre signed url {}", requestObject.url());
        LOG.debug("Generated pre signed details meta {}", requestObject.signedHeaders());


        return response(apiGatewayProxyResponseEvent, fileName, requestObject.url());
    }

    @PowertoolsTracing
    private APIGatewayProxyResponseEvent response(final APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent,
                                                  final String fileName,
                                                  final URL url) {
        try {
            return apiGatewayProxyResponseEvent
                    .withStatusCode(200)
                    .withBody(OBJECT_MAPPER.writeValueAsString(new ResponseBody(url.toString(), fileName)));
        } catch (JsonProcessingException e) {
            LOG.error("Failed sending response", e);
            throw new RuntimeException(e);
        }
    }

    public class ResponseBody {
        private String uploadURL;
        private String fileName;

        public ResponseBody(String uploadURL, String fileName) {
            this.uploadURL = uploadURL;
            this.fileName = fileName;
        }

        public String getUploadURL() {
            return uploadURL;
        }

        public void setUploadURL(String uploadURL) {
            this.uploadURL = uploadURL;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
