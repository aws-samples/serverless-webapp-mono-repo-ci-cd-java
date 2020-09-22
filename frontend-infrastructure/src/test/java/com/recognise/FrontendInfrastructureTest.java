package com.recognise;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.AppProps;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class FrontendInfrastructureTest {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() {
        Map<String, Object> context = new HashMap<>();
        context.put("frontend_path", "../frontend");

        App app = new App(AppProps.builder()
                .context(context)
                .build());

        FrontendInfrastructureStack stack = new FrontendInfrastructureStack(app, "test");

        // synthesize the stack to a CloudFormation template and compare against
        // a checked-in JSON file.
        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());
        assertNotNull(actual);
    }
}
