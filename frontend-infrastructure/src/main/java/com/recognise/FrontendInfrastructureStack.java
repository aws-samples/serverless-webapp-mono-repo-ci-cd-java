package com.recognise;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.cloudfront.Behavior;
import software.amazon.awscdk.services.cloudfront.CfnDistribution;
import software.amazon.awscdk.services.cloudfront.CloudFrontWebDistribution;
import software.amazon.awscdk.services.cloudfront.OriginAccessIdentity;
import software.amazon.awscdk.services.cloudfront.S3OriginConfig;
import software.amazon.awscdk.services.cloudfront.SourceConfiguration;
import software.amazon.awscdk.services.cloudfront.ViewerCertificate;
import software.amazon.awscdk.services.cloudfront.ViewerCertificateOptions;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.RecordSet;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.RecordType;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.ISource;
import software.amazon.awscdk.services.s3.deployment.Source;

import java.util.List;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.core.RemovalPolicy.DESTROY;
import static software.amazon.awscdk.services.certificatemanager.ValidationMethod.DNS;


public class FrontendInfrastructureStack extends Stack {
    public FrontendInfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public FrontendInfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Bucket frontendBucket = Bucket.Builder.create(this, "WebApplicationFrontendBucket")
                .websiteIndexDocument("index.html")
                .websiteErrorDocument("index.html")
                .removalPolicy(DESTROY)
                .build();

        OriginAccessIdentity accessIdentity = OriginAccessIdentity.Builder.create(this, "FrontEndOriginAccess")
                .comment("Allows Read Access from cloudfront")
                .build();

        frontendBucket.grantRead(accessIdentity);

        HostedZone hostedZone = null;
        DnsValidatedCertificate certificate = null;

        if (!contextValue("cert_domain").isEmpty()) {
            hostedZone = hostedZone();
            certificate = certificate(hostedZone);
        }

        CloudFrontWebDistribution distribution = distribution(frontendBucket, accessIdentity, certificate);

        if (null != hostedZone) {
            recordSets(hostedZone, distribution);
        }

        cloudFrontDeployment(frontendBucket, distribution);
    }

    private HostedZone hostedZone() {
        return HostedZone.Builder.create(this, contextValue("cert_domain"))
                .zoneName(contextValue("cert_domain"))
                .comment("Root for domain")
                .build();
    }

    private DnsValidatedCertificate certificate(HostedZone hostedZone) {
        return DnsValidatedCertificate.Builder.create(this, "WebApplicationWebsite")
                .domainName("*." + contextValue("cert_domain"))
                .hostedZone(hostedZone)
                .region("us-east-1")
                .validation(CertificateValidation.fromDns(hostedZone))
                .build();
    }

    private CloudFrontWebDistribution distribution(Bucket frontendBucket, OriginAccessIdentity accessIdentity, DnsValidatedCertificate certificate) {
        CloudFrontWebDistribution.Builder builder = CloudFrontWebDistribution.Builder.create(this, "WebApplicationFrontendDistribution")
                .originConfigs(singletonList(
                        SourceConfiguration.builder()
                                .s3OriginSource(S3OriginConfig.builder()
                                        .s3BucketSource(frontendBucket)
                                        .originAccessIdentity(accessIdentity)
                                        .build())
                                .behaviors(singletonList(Behavior.builder()
                                        .isDefaultBehavior(true)
                                        .build()))
                                .build()
                ))
                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                .errorConfigurations(singletonList(CfnDistribution.CustomErrorResponseProperty.builder()
                        .errorCode(404)
                        .responseCode(200)
                        .responsePagePath("/index.html")
                        .build()))
                .comment("Serve frontend for recognition")
                .defaultRootObject("/index.html");

        if (certificate != null) {
            ViewerCertificate cert_domain = ViewerCertificate.fromAcmCertificate(certificate, ViewerCertificateOptions.builder()
                    .aliases(singletonList("face-recog." + contextValue("cert_domain")))
                    .build());

            builder.viewerCertificate(cert_domain);
        }

        return builder
                .build();
    }

    private void recordSets(HostedZone hostedZone, CloudFrontWebDistribution distribution) {
        CloudFrontTarget cloudFrontTarget = new CloudFrontTarget(distribution);

        RecordSet.Builder.create(this, "face-recog")
                .comment("face-recog  root")
                .recordName("face-recog")
                .recordType(RecordType.A)
                .zone(hostedZone)
                .target(RecordTarget.fromAlias(cloudFrontTarget))
                .build();
    }

    private void cloudFrontDeployment(Bucket frontendBucket, CloudFrontWebDistribution distribution) {
        List<ISource> sources = singletonList(Source.asset(contextValue("frontend_path") + "/build"));

        BucketDeployment.Builder.create(this, "DeployWithInvalidationSite")
                .sources(sources)
                .destinationBucket(frontendBucket)
                .distribution(distribution)
                .build();
    }

    private String contextValue(String key) {
        return String.valueOf(this.getNode().tryGetContext(key));
    }
}
