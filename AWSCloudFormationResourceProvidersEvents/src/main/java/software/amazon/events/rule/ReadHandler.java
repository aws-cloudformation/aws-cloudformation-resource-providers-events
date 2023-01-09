package software.amazon.events.rule;

// TODO: replace all usage of SdkClient with your service client type, e.g; YourServiceAsyncClient
// import software.amazon.awssdk.services.yourservice.YourServiceAsyncClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import software.amazon.awssdk.services.cloudwatchevents.CloudWatchEventsClient;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
//import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
//import software.amazon.awssdk.services.cloudwatchevents.model.Target;
//import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandTarget;
//import software.amazon.events.rule.RunCommandTarget;
//import software.amazon.events.rule.Target;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;
    private static final ObjectMapper objectMapper = new ObjectMapper();


    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudWatchEventsClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        ResourceModel.ResourceModelBuilder finalResourceModel = ResourceModel.builder();



        //DescribeRuleResponse awsResponse = null;


        // TODO: Adjust Progress Chain according to your implementation
        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        // STEP 1 [initialize a proxy context]

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> proxy.initiate("AWS-Events-Rule::Read", proxyClient, request.getDesiredResourceState(), callbackContext)

                        // STEP 2 [TODO: construct a body of a request]
                        .translateToServiceRequest(Translator::translateToReadRequest)

                        // STEP 3 [TODO: make an api call]
                        // Implement client invocation of the read request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        .makeServiceCall((awsRequest, client) -> {
                            DescribeRuleResponse awsResponse = null;
                            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeRule);

                            logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                            return awsResponse;
                        })

                        // TODO
                        //.handleError()
                        // throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e); // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/commit/2077c92299aeb9a68ae8f4418b5e932b12a8b186#diff-5761e3a9f732dc1ef84103dc4bc93399R56-R63

                        // STEP 4 [TODO: gather all properties of the resource]
                        // Implement client invocation of the read request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        .done(awsResponse -> {
                            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
                            HashMap<String, Object> eventPattern;

                            try {
                                eventPattern = objectMapper.readValue(
                                        awsResponse.eventPattern(),
                                        typeRef
                                );
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }

                            finalResourceModel.description(awsResponse.description())
                                    .eventBusName(awsResponse.eventBusName())
                                    .eventPattern(eventPattern)
                                    .name(awsResponse.name())
                                    .roleArn(awsResponse.roleArn())
                                    .scheduleExpression(awsResponse.scheduleExpression())
                                    .state(awsResponse.scheduleExpression());

                            return ProgressEvent.progress(model, callbackContext);

                        }))

                .then(progress -> proxy.initiate("AWS-Events-Rule::ListTargets", proxyClient, request.getDesiredResourceState(), callbackContext)

                        // STEP 2 [TODO: construct a body of a request]
                        .translateToServiceRequest(Translator::translateToReadRequestListTargets)

                        // STEP 3 [TODO: make an api call]
                        // Implement client invocation of the read request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        .makeServiceCall((awsRequest, client) -> {
                            ListTargetsByRuleResponse awsResponse = null;
                            awsResponse = proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listTargetsByRule);

                            // TODO
                            //.handleError()
                            //throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e); // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/commit/2077c92299aeb9a68ae8f4418b5e932b12a8b186#diff-5761e3a9f732dc1ef84103dc4bc93399R56-R63

                            logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                            return awsResponse;
                        })

                        // STEP 4 [TODO: gather all properties of the resource]
                        // Implement client invocation of the read request through the proxyClient, which is already initialised with
                        // caller credentials, correct region and retry settings
                        .done(awsResponse -> {
                            Set<Target> targets = new HashSet<>();

                            if (awsResponse.targets() != null) {

                                for (software.amazon.awssdk.services.cloudwatchevents.model.Target target : awsResponse.targets()) {
                                    Target thisTarget = new Target();

                                    Set<RunCommandTarget> runCommandTargets = new HashSet<>();

                                    BatchParameters.BatchParametersBuilder batchParametersBuilder = new BatchParameters.BatchParametersBuilder();
                                    BatchRetryStrategy.BatchRetryStrategyBuilder batchRetryStrategyBuilder = new BatchRetryStrategy.BatchRetryStrategyBuilder();
                                    BatchArrayProperties.BatchArrayPropertiesBuilder batchArrayPropertiesBuilder = new BatchArrayProperties.BatchArrayPropertiesBuilder();
                                    DeadLetterConfig.DeadLetterConfigBuilder deadLetterConfigBuilder = new DeadLetterConfig.DeadLetterConfigBuilder();
                                    EcsParameters.EcsParametersBuilder ecsParametersBuilder = new EcsParameters.EcsParametersBuilder();
                                    NetworkConfiguration.NetworkConfigurationBuilder networkConfigurationBuilder = new NetworkConfiguration.NetworkConfigurationBuilder();
                                    AwsVpcConfiguration.AwsVpcConfigurationBuilder awsVpcConfigurationBuilder = new AwsVpcConfiguration.AwsVpcConfigurationBuilder();
                                    HttpParameters.HttpParametersBuilder httpParametersBuilder = new HttpParameters.HttpParametersBuilder();
                                    InputTransformer.InputTransformerBuilder inputTransformerBuilder = new InputTransformer.InputTransformerBuilder();
                                    KinesisParameters.KinesisParametersBuilder kinesisParametersBuilder = new KinesisParameters.KinesisParametersBuilder();
                                    RedshiftDataParameters.RedshiftDataParametersBuilder redshiftDataParametersBuilder = new RedshiftDataParameters.RedshiftDataParametersBuilder();
                                    RetryPolicy.RetryPolicyBuilder retryPolicyBuilder = new RetryPolicy.RetryPolicyBuilder();
                                    RunCommandParameters.RunCommandParametersBuilder runCommandParametersBuilder = new RunCommandParameters.RunCommandParametersBuilder();
                                    SqsParameters.SqsParametersBuilder sqsParametersBuilder = new SqsParameters.SqsParametersBuilder();

                                    if (target.batchParameters() != null) {

                                        batchRetryStrategyBuilder.attempts(target.batchParameters().retryStrategy() != null
                                                ? target.batchParameters().retryStrategy().attempts()
                                                : null);

                                        batchArrayPropertiesBuilder.size(target.batchParameters().arrayProperties() != null
                                                ? target.batchParameters().arrayProperties().size()
                                                : null);

                                        batchParametersBuilder.jobName(target.batchParameters().jobName());
                                        batchParametersBuilder.retryStrategy(batchRetryStrategyBuilder.build());
                                        batchParametersBuilder.arrayProperties(batchArrayPropertiesBuilder.build());
                                        batchParametersBuilder.jobDefinition(target.batchParameters().jobDefinition());

                                        thisTarget.setBatchParameters(batchParametersBuilder.build());
                                    }

                                    if (target.deadLetterConfig() != null) {
                                        deadLetterConfigBuilder.arn(target.deadLetterConfig().arn());

                                        thisTarget.setDeadLetterConfig(deadLetterConfigBuilder.build());
                                    }

                                    if (target.ecsParameters() != null) {
                                        if (target.ecsParameters().networkConfiguration() != null && target.ecsParameters().networkConfiguration().awsvpcConfiguration() != null) {
                                            awsVpcConfigurationBuilder.securityGroups(target.ecsParameters().networkConfiguration().awsvpcConfiguration().securityGroups() != null
                                                    ? new HashSet<>(target.ecsParameters().networkConfiguration().awsvpcConfiguration().securityGroups())
                                                    : null);
                                            awsVpcConfigurationBuilder.subnets(target.ecsParameters().networkConfiguration().awsvpcConfiguration().subnets() != null
                                                    ? new HashSet<>(target.ecsParameters().networkConfiguration().awsvpcConfiguration().subnets())
                                                    : null);
                                            awsVpcConfigurationBuilder.assignPublicIp(target.ecsParameters().networkConfiguration().awsvpcConfiguration().assignPublicIpAsString());

                                            networkConfigurationBuilder.awsVpcConfiguration(awsVpcConfigurationBuilder.build());
                                        }

                                        ecsParametersBuilder.platformVersion(target.ecsParameters().platformVersion());
                                        ecsParametersBuilder.group(target.ecsParameters().group());
                                        ecsParametersBuilder.taskCount(target.ecsParameters().taskCount());
                                        ecsParametersBuilder.launchType(target.ecsParameters().launchTypeAsString());
                                        ecsParametersBuilder.networkConfiguration(networkConfigurationBuilder.build());
                                        ecsParametersBuilder.taskDefinitionArn(target.ecsParameters().taskDefinitionArn());

                                        thisTarget.setEcsParameters(ecsParametersBuilder.build());
                                    }

                                    if (target.httpParameters() != null) {
                                        httpParametersBuilder.pathParameterValues(target.httpParameters().pathParameterValues() != null
                                                ? new HashSet<>(target.httpParameters().pathParameterValues())
                                                : null);
                                        httpParametersBuilder.headerParameters(target.httpParameters().headerParameters());
                                        httpParametersBuilder.queryStringParameters(target.httpParameters().queryStringParameters());

                                        thisTarget.setHttpParameters(httpParametersBuilder.build());
                                    }

                                    if (target.inputTransformer() != null) {
                                        inputTransformerBuilder.inputTemplate(target.inputTransformer().inputTemplate());
                                        inputTransformerBuilder.inputPathsMap(target.inputTransformer().inputPathsMap());

                                        thisTarget.setInputTransformer(inputTransformerBuilder.build());
                                    }

                                    if (target.kinesisParameters() != null) {
                                        kinesisParametersBuilder.partitionKeyPath(target.kinesisParameters().partitionKeyPath());

                                        thisTarget.setKinesisParameters(kinesisParametersBuilder.build());
                                    }

                                    if (target.redshiftDataParameters() != null) {
                                        redshiftDataParametersBuilder.statementName(target.redshiftDataParameters().statementName());
                                        redshiftDataParametersBuilder.database(target.redshiftDataParameters().database());
                                        redshiftDataParametersBuilder.secretManagerArn(target.redshiftDataParameters().secretManagerArn());
                                        redshiftDataParametersBuilder.dbUser(target.redshiftDataParameters().dbUser());
                                        redshiftDataParametersBuilder.sql(target.redshiftDataParameters().sql());
                                        redshiftDataParametersBuilder.withEvent(target.redshiftDataParameters().withEvent());

                                        thisTarget.setRedshiftDataParameters(redshiftDataParametersBuilder.build());
                                    }

                                    if (target.retryPolicy() != null) {
                                        retryPolicyBuilder.maximumEventAgeInSeconds(target.retryPolicy().maximumEventAgeInSeconds());
                                        retryPolicyBuilder.maximumRetryAttempts(target.retryPolicy().maximumRetryAttempts());

                                        thisTarget.setRetryPolicy(retryPolicyBuilder.build());
                                    }

                                    if (target.runCommandParameters() != null) {
                                        for (software.amazon.awssdk.services.cloudwatchevents.model.RunCommandTarget runCommandTarget : target.runCommandParameters().runCommandTargets()) {
                                            runCommandTargets.add(new RunCommandTarget.RunCommandTargetBuilder()
                                                    .values(new HashSet<>(runCommandTarget.values()))
                                                    .key(runCommandTarget.key())
                                                    .build()
                                            );
                                        }

                                        if (runCommandTargets.size() > 0) {
                                            runCommandParametersBuilder.runCommandTargets(runCommandTargets);

                                            thisTarget.setRunCommandParameters(runCommandParametersBuilder.build());
                                        }
                                    }

                                    if (target.sqsParameters() != null) {
                                        sqsParametersBuilder.messageGroupId(target.sqsParameters().messageGroupId());

                                        thisTarget.setSqsParameters(sqsParametersBuilder.build());
                                    }

                                    thisTarget.setArn(target.arn());
                                    thisTarget.setId(target.id());
                                    thisTarget.setInput(target.input());
                                    thisTarget.setInputPath(target.inputPath());
                                    thisTarget.setRoleArn(target.roleArn());
                                    //thisTarget.setSageMakerPipelineParameters(target.sageMakerPipelineParameters()); FIXME Not supported

                                    targets.add(thisTarget);
                                }

                                finalResourceModel.targets(targets);
                            }

                            return ProgressEvent.defaultSuccessHandler(finalResourceModel.build());
                        }));
    }
}
