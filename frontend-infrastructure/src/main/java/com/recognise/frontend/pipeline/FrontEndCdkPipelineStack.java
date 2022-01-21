package com.recognise.frontend.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.pipelines.AddStageOpts;
import software.amazon.awscdk.pipelines.CodeBuildOptions;
import software.amazon.awscdk.pipelines.CodeBuildStep;
import software.amazon.awscdk.pipelines.CodeBuildStepProps;
import software.amazon.awscdk.pipelines.CodePipeline;
import software.amazon.awscdk.pipelines.CodePipelineSource;
import software.amazon.awscdk.pipelines.ManualApprovalStep;
import software.amazon.awscdk.pipelines.S3SourceOptions;
import software.amazon.awscdk.pipelines.Step;
import software.amazon.awscdk.services.cloudtrail.ReadWriteType;
import software.amazon.awscdk.services.cloudtrail.S3EventSelector;
import software.amazon.awscdk.services.cloudtrail.Trail;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.EventAction;
import software.amazon.awscdk.services.codebuild.FilterGroup;
import software.amazon.awscdk.services.codebuild.GitHubSourceProps;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codebuild.Source;
import software.amazon.awscdk.services.codepipeline.actions.S3Trigger;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.services.codebuild.ComputeType.LARGE;
import static software.amazon.awscdk.services.codebuild.LinuxBuildImage.AMAZON_LINUX_2_ARM;

public class FrontEndCdkPipelineStack extends Stack {

    public FrontEndCdkPipelineStack(Construct scope, String id, StackProps props) {
        super(scope, id, props);

        Topic topic = null;

        if (!contextValue("approval_emails").isEmpty()) {
            String[] approval_emails = contextValue("approval_emails").split(",");
            topic = Topic.Builder.create(this, "ApprovalNotificationTopic")
                    .displayName("ApprovalNotificationTopic")
                    .topicName("ApprovalNotificationTopic")
                    .build();


            for (String email : approval_emails) {
                topic.addSubscription(EmailSubscription.Builder.create(email).build());
            }
        }

        Bucket frontEndArtifactBucket = Bucket.Builder.create(this, "FrontEndArtifactBucket")
                .versioned(true)
                .build();

        Trail frontEndTrail = Trail.Builder.create(this, "FrontEndCodeCdkPipelineAwsCloudTrail")
                .isMultiRegionTrail(true)
                .trailName("FrontEndCodeCdkPipelineAwsCloudTrail")
                .managementEvents(ReadWriteType.WRITE_ONLY)
                .includeGlobalServiceEvents(true)
                .build();

        String sourceZip = "frontend-source.zip";

        frontEndTrail.addS3EventSelector(singletonList(S3EventSelector.builder()
                .bucket(frontEndArtifactBucket)
                .objectPrefix(sourceZip)
                .build()));

        HashMap<String, BuildEnvironmentVariable> envVar = new HashMap<>();

        envVar.put("SOURCE_OBJECT_KEY", BuildEnvironmentVariable.builder()
                .value(sourceZip)
                .build());

        envVar.put("SOURCE_OUTPUT_BUCKET", BuildEnvironmentVariable.builder()
                .value(frontEndArtifactBucket.getBucketName())
                .build());

        envVar.put("FOLDER_TO_INCLUDE", BuildEnvironmentVariable.builder()
                .value("frontend frontend-infrastructure")
                .build());

        Project detectorBuild = Project.Builder.create(this, "WebApplicationFrontEndCdkPipelineDetector")
                .projectName("WebApplicationFrontEndCdkPipelineDetector")
                .description("Detects if frontend or frontend infra is updated. It will then invoke frontend pipeline")
                .buildSpec(BuildSpec.fromSourceFilename("buildspec-copyartifact.yaml"))
                .environment(BuildEnvironment.builder()
                        .computeType(LARGE)
                        .buildImage(AMAZON_LINUX_2_ARM)
                        .build())
                .source(Source.gitHub(GitHubSourceProps.builder()
                        .cloneDepth(0)
                        .owner(contextValue("owner"))
                        .repo(contextValue("repo"))
                        .webhookFilters(asList(FilterGroup.inEventOf(EventAction.PUSH)
                                .andFilePathIs("frontend/"), FilterGroup.inEventOf(EventAction.PUSH)
                                .andFilePathIs("frontend-infrastructure/")))
                        .webhook(true)
                        .build()))
                .environmentVariables(envVar)
                .build();

        detectorBuild.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(asList(
                        "s3:PutObject",
                        "s3:GetObject",
                        "s3:GetObjectVersion"))
                .resources(singletonList(frontEndArtifactBucket.getBucketArn() + "/*"))
                .build());

        detectorBuild.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(singletonList(
                        "s3:ListBucket"))
                .resources(singletonList(frontEndArtifactBucket.getBucketArn()))
                .build());

        CodePipeline codePipeline = CodePipeline.Builder.create(this, "WebApplicationFrontEndCdkPipeline")
                .pipelineName("WebApplicationFrontEndCdkPipeline")
                .crossAccountKeys(true)
                // Self mutation is very much needed here coz of https://github.com/aws/aws-cdk/issues/9080.
                // Else the publishing actions might try to publish obsolete hash
                .selfMutation(true)
                .synth(new CodeBuildStep("BuildFrontendInfrastructureProject", CodeBuildStepProps.builder()
                        //.partialBuildSpec(BuildSpec.fromSourceFilename(contextValue("frontend_infra_build_spec")))
                        .commands(asList(
                                "cd frontend",
                                "npm install --global npm && npm ci",
                                "npm run build",
                                "CI=true npm test",
                                "cd ../frontend-infrastructure",
                                "npm install -g aws-cdk",
                                "mvn clean install --quiet",
                                "cdk synth"
                        ))
                        .primaryOutputDirectory("./frontend-infrastructure/cdk.out")
                        .input(CodePipelineSource.s3(frontEndArtifactBucket, sourceZip, S3SourceOptions.builder()
                                .trigger(S3Trigger.EVENTS)
                                .actionName("S3FrontEndSource")
                                .build()))
                        .projectName("BuildFrontendInfrastructureProject")
                        .build()))
                .codeBuildDefaults(CodeBuildOptions.builder()
                        .buildEnvironment(BuildEnvironment.builder()
                                .computeType(LARGE)
                                .buildImage(AMAZON_LINUX_2_ARM)
                                .build())
                        .build())
                .build();

        List<Step> preProStep = new ArrayList<>();

        if (null != topic) {
            preProStep.add(ManualApprovalStep.Builder.create("Approval")
                    .comment("Approve before site is deployed for public.")
                    .build());
        }

        codePipeline.addStage(new ApplicationStage(this, "prod", software.amazon.awscdk.core.StageProps.builder()
                .env(Environment.builder()
                        .region(props.getEnv().getRegion())
                        .account(props.getEnv().getAccount())
                        .build())
                .build()), AddStageOpts.builder()
                .pre(preProStep)
                .build());

        codePipeline.addStage(new ApplicationStage(this, "post-prod", software.amazon.awscdk.core.StageProps.builder()
                .env(Environment.builder()
                        .region(props.getEnv().getRegion())
                        .account(props.getEnv().getAccount())
                        .build())
                .build()), AddStageOpts.builder()
                .pre(preProStep)
                .build());
    }

    private String contextValue(String key) {
        return String.valueOf(this.getNode().tryGetContext(key));
    }
}
