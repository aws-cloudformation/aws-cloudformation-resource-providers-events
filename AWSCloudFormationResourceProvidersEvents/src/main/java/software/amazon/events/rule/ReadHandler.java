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

                            for (software.amazon.awssdk.services.cloudwatchevents.model.Target target : awsResponse.targets())
                            {
                                Target thisTarget = new Target();

                                Set<RunCommandTarget> runCommandTargets = new HashSet<>();

                                for (software.amazon.awssdk.services.cloudwatchevents.model.RunCommandTarget runCommandTarget : target.runCommandParameters().runCommandTargets()) {
                                    runCommandTargets.add(new RunCommandTarget.RunCommandTargetBuilder()
                                            .values(new HashSet<>(runCommandTarget.values()))
                                            .key(runCommandTarget.key())
                                            .build()
                                    );
                                }

                                thisTarget.setArn(target.arn());
                                thisTarget.setBatchParameters(new BatchParameters.BatchParametersBuilder()
                                        .jobName(target.batchParameters().jobName())
                                        .retryStrategy(new BatchRetryStrategy.BatchRetryStrategyBuilder()
                                                .attempts(target.batchParameters().retryStrategy().attempts())
                                                .build())
                                        .arrayProperties(new BatchArrayProperties.BatchArrayPropertiesBuilder()
                                                .size(target.batchParameters().arrayProperties().size())
                                                .build())
                                        .jobDefinition(target.batchParameters().jobDefinition())
                                        .build()
                                );
                                thisTarget.setDeadLetterConfig(new DeadLetterConfig.DeadLetterConfigBuilder()
                                        .arn(target.deadLetterConfig().arn())
                                        .build()
                                );
                                thisTarget.setEcsParameters(new EcsParameters.EcsParametersBuilder()
                                        .platformVersion(target.ecsParameters().platformVersion())
                                        .group(target.ecsParameters().group())
                                        .taskCount(target.ecsParameters().taskCount())
                                        .launchType(target.ecsParameters().launchTypeAsString())
                                        .networkConfiguration(new NetworkConfiguration.NetworkConfigurationBuilder()
                                                .awsVpcConfiguration(new AwsVpcConfiguration.AwsVpcConfigurationBuilder()
                                                        .securityGroups(new HashSet<>(target.ecsParameters().networkConfiguration().awsvpcConfiguration().securityGroups()))
                                                        .subnets(new HashSet<>(target.ecsParameters().networkConfiguration().awsvpcConfiguration().subnets()))
                                                        .assignPublicIp(target.ecsParameters().networkConfiguration().awsvpcConfiguration().assignPublicIpAsString())
                                                        .build())
                                                .build())
                                        .taskDefinitionArn(target.ecsParameters().taskDefinitionArn())
                                        .build()
                                );
                                thisTarget.setHttpParameters(new HttpParameters.HttpParametersBuilder()
                                        .pathParameterValues(new HashSet<>(target.httpParameters().pathParameterValues()))
                                        .headerParameters(target.httpParameters().headerParameters())
                                        .queryStringParameters(target.httpParameters().queryStringParameters())
                                        .build()
                                );
                                thisTarget.setId(target.id());
                                thisTarget.setInput(target.input());
                                thisTarget.setInputPath(target.inputPath());
                                thisTarget.setInputTransformer(new InputTransformer.InputTransformerBuilder()
                                        .inputTemplate(target.inputTransformer().inputTemplate())
                                        .inputPathsMap(target.inputTransformer().inputPathsMap())
                                        .build()
                                );
                                thisTarget.setKinesisParameters(new KinesisParameters.KinesisParametersBuilder()
                                        .partitionKeyPath(target.kinesisParameters().partitionKeyPath())
                                        .build()
                                );
                                thisTarget.setRedshiftDataParameters(new RedshiftDataParameters.RedshiftDataParametersBuilder()
                                        .statementName(target.redshiftDataParameters().statementName())
                                        .database(target.redshiftDataParameters().database())
                                        .secretManagerArn(target.redshiftDataParameters().secretManagerArn())
                                        .dbUser(target.redshiftDataParameters().dbUser())
                                        .sql(target.redshiftDataParameters().sql())
                                        .withEvent(target.redshiftDataParameters().withEvent())
                                        .build()
                                );
                                thisTarget.setRetryPolicy(new RetryPolicy.RetryPolicyBuilder()
                                        .maximumEventAgeInSeconds(target.retryPolicy().maximumEventAgeInSeconds())
                                        .maximumRetryAttempts(target.retryPolicy().maximumRetryAttempts())
                                        .build()
                                );
                                thisTarget.setRoleArn(target.roleArn());
                                thisTarget.setRunCommandParameters(new RunCommandParameters.RunCommandParametersBuilder()
                                        .runCommandTargets(runCommandTargets)
                                        .build()
                                );
                                //thisTarget.setSageMakerPipelineParameters(target.sageMakerPipelineParameters()); FIXME Not supported
                                thisTarget.setSqsParameters(new SqsParameters.SqsParametersBuilder()
                                        .messageGroupId(target.sqsParameters().messageGroupId())
                                        .build()
                                );

                                targets.add(thisTarget);
                            }

                            finalResourceModel.targets(targets);

                            return ProgressEvent.defaultSuccessHandler(finalResourceModel.build());
                        }));
    }
}
