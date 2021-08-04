package com.recognise;

import com.recognise.frontend.pipeline.FrontEndCdkPipelineStack;
import com.recognise.frontend.pipeline.FrontEndPipelineStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class FrontendInfrastructureApp {
    public static void main(final String[] args) {
        App app = new App();

        new FrontendInfrastructureStack(app, "serverless-web-application-frontend", StackProps.builder()
                .description("Creates the frontend infrastructure for content")
                .env(Environment.builder()
                        .region("eu-west-1")
                        .build())
                .build());


        new FrontEndPipelineStack(app, "serverless-web-application-frontend-pipeline", StackProps.builder()
                .description("Creates the frontend infrastructure pipeline")
                .env(Environment.builder()
                        .region("eu-west-1")
                        .build())
                .build());

        new FrontEndCdkPipelineStack(app, "serverless-web-application-frontend-cdk-pipeline", StackProps.builder()
                .description("Creates the frontend infrastructure using cdk pipeline")
                .env(Environment.builder()
                        .region("eu-west-1")
                        .build())
                .build());

        app.synth();
    }
}
