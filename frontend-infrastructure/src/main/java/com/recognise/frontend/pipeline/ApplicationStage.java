package com.recognise.frontend.pipeline;

import java.util.Optional;

import com.recognise.FrontendInfrastructureStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Stage;
import software.amazon.awscdk.core.StageProps;
import software.constructs.Construct;

import static java.util.Optional.*;

public class ApplicationStage extends Stage {

    public ApplicationStage(@NotNull Construct scope, @NotNull String id, @Nullable StageProps props) {
        super(scope, id, props);

        new FrontendInfrastructureStack(this, "web-application-frontend", StackProps.builder()
                .description("Creates the frontend infrastructure for content")
                .env(Environment.builder()
                        .region(ofNullable(props)
                                .map(StageProps::getEnv)
                                .map(Environment::getRegion)
                                .orElse("eu-west-1"))
                        .build())
                .build());
    }

}
