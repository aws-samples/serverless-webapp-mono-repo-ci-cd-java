package com.recognise.frontend.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.cloudtrail.ReadWriteType;
import software.amazon.awscdk.services.cloudtrail.Trail;
import software.amazon.awscdk.services.codebuild.BuildEnvironment;
import software.amazon.awscdk.services.codebuild.BuildEnvironmentVariable;
import software.amazon.awscdk.services.codebuild.BuildSpec;
import software.amazon.awscdk.services.codebuild.ComputeType;
import software.amazon.awscdk.services.codebuild.EventAction;
import software.amazon.awscdk.services.codebuild.FilterGroup;
import software.amazon.awscdk.services.codebuild.GitHubSourceProps;
import software.amazon.awscdk.services.codebuild.PipelineProject;
import software.amazon.awscdk.services.codebuild.Project;
import software.amazon.awscdk.services.codebuild.Source;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.IAction;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.CodeBuildAction;
import software.amazon.awscdk.services.codepipeline.actions.ManualApprovalAction;
import software.amazon.awscdk.services.codepipeline.actions.S3SourceAction;
import software.amazon.awscdk.services.codepipeline.actions.S3Trigger;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.amazon.awscdk.services.codebuild.ComputeType.LARGE;
import static software.amazon.awscdk.services.codebuild.LinuxBuildImage.AMAZON_LINUX_2_ARM;
import static software.amazon.awscdk.services.codepipeline.actions.CodeBuildActionType.BUILD;

public class FrontEndPipelineStack extends Stack {

    public FrontEndPipelineStack(Construct scope, String id, StackProps props) {
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

        Trail frontEndTrail = Trail.Builder.create(this, "FrontEndCodeAwsCloudTrail")
                .isMultiRegionTrail(true)
                .trailName("FrontEndCodeAwsCloudTrail")
                .managementEvents(ReadWriteType.WRITE_ONLY)
                .includeGlobalServiceEvents(true)
                .build();

        String sourceZip = "frontend-source.zip";

        frontEndTrail.addS3EventSelector(singletonList(frontEndArtifactBucket.getBucketArn() + "/" + sourceZip));

        Artifact sourceOutput = Artifact.artifact("FrontEndSourceOutput");
        Artifact buildOutput = Artifact.artifact("FrontEndBuildOutput");
        Artifact cdkBuildOutput = Artifact.artifact("FrontEndInfraOutput");

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

        Project detectorBuild = Project.Builder.create(this, "DetectorFrontEndBuildProject")
                .projectName("WebApplicationFrontEndPipelineDetector")
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

        PipelineProject codeBuildFrontEndProject = PipelineProject.Builder.create(this, "WebApplicationFrontEndBuild")
                .projectName("WebApplicationFrontEndBuild")
                .description("Builds frontend website")
                .buildSpec(BuildSpec.fromSourceFilename(contextValue("frontend_build_spec")))
                .environment(BuildEnvironment.builder()
                        .computeType(LARGE)
                        .buildImage(AMAZON_LINUX_2_ARM)
                        .build())
                .build();

        PipelineProject codeBuildFrontEndInfraProject = PipelineProject.Builder.create(this, "WebApplicationFrontEndInfrastructureBuild")
                .projectName("WebApplicationFrontEndInfrastructureBuild")
                .description("Builds frontend infrastructure")
                .buildSpec(BuildSpec.fromSourceFilename(contextValue("frontend_infra_build_spec")))
                .environment(BuildEnvironment.builder()
                        .computeType(LARGE)
                        .buildImage(AMAZON_LINUX_2_ARM)
                        .build())
                .build();

        Role adminRole = Role.Builder.create(this, "WebApplicationCodeBuildExecutionRole")
                .assumedBy(ServicePrincipal.Builder.create("codebuild.amazonaws.com")
                        .build())
                .managedPolicies(singletonList(ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess")))
                .build();

        PipelineProject codeBuildFrontEndInfraDeployProject = PipelineProject.Builder.create(this, "WebApplicationFrontEndInfrastructureDeploy")
                .projectName("WebApplicationFrontEndInfrastructureDeploy")
                .role(adminRole)
                .description("Deploys frontend infrastructure and website")
                .buildSpec(BuildSpec.fromSourceFilename(contextValue("frontend_infra_deploy_spec")))
                .environment(BuildEnvironment.builder()
                        .computeType(LARGE)
                        .buildImage(AMAZON_LINUX_2_ARM)
                        .build())
                .build();

        codeBuildFrontEndInfraProject.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(asList(
                        "cloudformation:DescribeStacks",
                        "cloudformation:GetTemplate"
                        )
                )
                .resources(singletonList("*"))
                .build());

        StageProps source = StageProps.builder()
                .stageName("Source")
                .actions(singletonList(
                        S3SourceAction.Builder.create()
                                .actionName("S3FrontEndSource")
                                .bucket(frontEndArtifactBucket)
                                .bucketKey(sourceZip)
                                .output(sourceOutput)
                                .trigger(S3Trigger.EVENTS)
                                .build()
                )).build();

        StageProps build = StageProps.builder()
                .stageName("Build")
                .actions(singletonList(
                        CodeBuildAction.Builder.create()
                                .actionName("FrontEndBuild")
                                .input(sourceOutput)
                                .type(BUILD)
                                .project(codeBuildFrontEndProject)
                                .outputs(singletonList(buildOutput))
                                .build()
                )).build();

        CodeBuildAction cdkAction = CodeBuildAction.Builder.create()
                .actionName("FrontEndInfrastructureBuild")
                .input(buildOutput)
                .type(BUILD)
                .project(codeBuildFrontEndInfraProject)
                .outputs(singletonList(cdkBuildOutput))
                .build();

        StageProps infraBuild = StageProps.builder()
                .stageName("InfraBuild")
                .actions(singletonList(cdkAction))
                .build();

        List<IAction> approvalAndDeploy = new ArrayList<>();

        if (null != topic) {
            ManualApprovalAction approval = ManualApprovalAction.Builder.create()
                    .actionName("Approval")
                    .runOrder(1)
                    .additionalInformation("Approve before site is deployed for public.")
                    .notificationTopic(topic)
                    .build();

            approvalAndDeploy.add(approval);
        }

        CodeBuildAction cdkDeployAction = CodeBuildAction.Builder.create()
                .actionName("FrontEndInfrastructureBuild")
                .runOrder(2)
                .input(cdkBuildOutput)
                .type(BUILD)
                .project(codeBuildFrontEndInfraDeployProject)
                .build();

        approvalAndDeploy.add(cdkDeployAction);

        StageProps approveAndDeploy = StageProps.builder()
                .stageName("DeploySiteAndInfrastructure")
                .actions(approvalAndDeploy)
                .build();

        Pipeline.Builder.create(this, "WebApplicationFrontendPipeline")
                .pipelineName("WebApplicationFrontEndPipeline")
                .artifactBucket(frontEndArtifactBucket)
                .stages(asList(
                        source,
                        build,
                        infraBuild,
                        approveAndDeploy
                        )
                ).build();
    }

    private String contextValue(String key) {
        return String.valueOf(this.getNode().tryGetContext(key));
    }
}
