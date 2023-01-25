package software.amazon.events.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import software.amazon.awssdk.services.cloudwatchevents.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.cloudwatchevents.model.BatchParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.BatchRetryStrategy;
import software.amazon.awssdk.services.cloudwatchevents.model.DeadLetterConfig;
import software.amazon.awssdk.services.cloudwatchevents.model.EcsParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.HttpParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.InputTransformer;
import software.amazon.awssdk.services.cloudwatchevents.model.KinesisParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.NetworkConfiguration;
import software.amazon.awssdk.services.cloudwatchevents.model.RedshiftDataParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.RetryPolicy;
import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.SqsParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.awssdk.services.cloudwatchevents.model.BatchArrayProperties;
import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandTarget;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.awssdk.services.cloudwatchevents.model.Tag;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static PutRuleRequest translateToPutRuleRequest(final ResourceModel model, Map<String, String> tags) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    String eventPattern = null;
    PutRuleRequest.Builder putRuleRequest = PutRuleRequest.builder();

    try {
      eventPattern = MAPPER.writeValueAsString(model.getEventPattern());
    } catch (final JsonProcessingException e) {
      throw new TerminalException(e);
    }

    return putRuleRequest
            .name(model.getName())
            .eventBusName(model.getEventBusName())
            .description(model.getDescription())
            .eventPattern(eventPattern)
            .roleArn(model.getRoleArn())
            .scheduleExpression(model.getScheduleExpression())
            .state(model.getState())
            .build();
  }

  static CloudWatchEventsRequest dummmyTranslator(final ResourceModel model) {
    return null;
  }

  static PutTargetsRequest translateToPutTargetsRequest(final ResourceModel model) {

    ArrayList<Target> targets = new ArrayList<Target>();

    for (software.amazon.events.rule.Target target : model.getTargets()) {
      Target.Builder targetBuilder = Target.builder();

      if (target.getBatchParameters() != null) {
        BatchParameters.Builder batchParameters = BatchParameters.builder();

        if (target.getBatchParameters().getArrayProperties() != null) {
          batchParameters.arrayProperties(BatchArrayProperties.builder()
                  .size(target.getBatchParameters().getArrayProperties().getSize())
                  .build());
        }

        if (target.getBatchParameters().getRetryStrategy() != null) {
          batchParameters.retryStrategy(BatchRetryStrategy.builder()
                  .attempts(target.getBatchParameters().getRetryStrategy().getAttempts())
                  .build());
        }

        targetBuilder.batchParameters(batchParameters
                .jobDefinition(target.getBatchParameters().getJobDefinition())
                .jobName(target.getBatchParameters().getJobName())
                .build());
      }

      if (target.getDeadLetterConfig() != null) {
        targetBuilder.deadLetterConfig(DeadLetterConfig.builder()
                .arn(target.getDeadLetterConfig().getArn())
                .build());
      }

      if (target.getEcsParameters() != null) {
        EcsParameters.Builder ecsParameters = EcsParameters.builder();

        if (target.getEcsParameters().getNetworkConfiguration() != null &&
                target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration() != null) {
          ecsParameters.networkConfiguration(NetworkConfiguration.builder()
                  .awsvpcConfiguration(AwsVpcConfiguration.builder()
                    .assignPublicIp(target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration().getAssignPublicIp())
                    .securityGroups(target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration().getSecurityGroups())
                    .subnets(target.getEcsParameters().getNetworkConfiguration().getAwsVpcConfiguration().getSubnets())
                    .build())
                  .build());
        }

        targetBuilder.ecsParameters(ecsParameters
                //.capacityProviderStrategy()
                //.enableECSManagedTags()
                //.enableExecuteCommand()
                .group(target.getEcsParameters().getGroup())
                .launchType(target.getEcsParameters().getLaunchType())
                //.placementConstraints()
                //.placementStrategy()
                .platformVersion(target.getEcsParameters().getPlatformVersion())
                //.propagateTags()
                //.referenceId()
                //.tags()
                .taskCount(target.getEcsParameters().getTaskCount())
                .taskDefinitionArn(target.getEcsParameters().getTaskDefinitionArn())
                .build());
      }

      if (target.getHttpParameters() != null) {
        targetBuilder.httpParameters(HttpParameters.builder()
                .headerParameters(target.getHttpParameters().getHeaderParameters())
                .pathParameterValues(target.getHttpParameters().getPathParameterValues())
                .queryStringParameters(target.getHttpParameters().getQueryStringParameters())
                .build());
      }

      if (target.getInputTransformer() != null) {
        targetBuilder.inputTransformer(InputTransformer.builder()
                .inputPathsMap(target.getInputTransformer().getInputPathsMap())
                .inputTemplate(target.getInputTransformer().getInputTemplate())
                .build());
      }

      if (target.getKinesisParameters() != null) {
        targetBuilder.kinesisParameters(KinesisParameters.builder()
                .partitionKeyPath(target.getKinesisParameters().getPartitionKeyPath())
                .build());
      }

      if (target.getRedshiftDataParameters() != null) {
        targetBuilder.redshiftDataParameters(RedshiftDataParameters.builder()
                .database(target.getRedshiftDataParameters().getDatabase())
                .dbUser(target.getRedshiftDataParameters().getDbUser())
                .secretManagerArn(target.getRedshiftDataParameters().getSecretManagerArn())
                .sql(target.getRedshiftDataParameters().getSql())
                .statementName(target.getRedshiftDataParameters().getStatementName())
                .withEvent(target.getRedshiftDataParameters().getWithEvent())
                .build());
      }

      if (target.getRetryPolicy() != null) {
        targetBuilder.retryPolicy(RetryPolicy.builder()
                .maximumEventAgeInSeconds(target.getRetryPolicy().getMaximumEventAgeInSeconds())
                .maximumRetryAttempts(target.getRetryPolicy().getMaximumRetryAttempts())
                .build());
      }

      if (target.getRunCommandParameters() != null && target.getRunCommandParameters().getRunCommandTargets() != null) {
        ArrayList<RunCommandTarget> runCommandTargets = new ArrayList<>();

        for (software.amazon.events.rule.RunCommandTarget value : target.getRunCommandParameters().getRunCommandTargets()) {
          runCommandTargets.add(RunCommandTarget.builder()
                  .key(value.getKey())
                  .values(value.getValues())
                  .build()
          );
        }

        targetBuilder.runCommandParameters(RunCommandParameters.builder()
                .runCommandTargets(runCommandTargets)
                .build());
      }

      if (target.getSqsParameters() != null) {
        targetBuilder.sqsParameters(SqsParameters.builder()
                .messageGroupId(target.getSqsParameters().getMessageGroupId())
                .build());
      }

      targets.add(Target.builder()
              .arn(target.getArn())
              .id(target.getId())
              .input(target.getInput())
              .inputPath(target.getInputPath())
              .roleArn(target.getRoleArn())
              //.sageMakerPipelineParameters(target.setSageMakerPipelineParameters())
              .build()
      );
    }

    return PutTargetsRequest.builder()
            .eventBusName(model.getEventBusName())
            .rule(model.getName())
            .targets(targets)
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeRuleRequest translateToDescribeRuleRequest(final ResourceModel model) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return DescribeRuleRequest.builder()
            .name(model.getName())
            .eventBusName(model.getEventBusName())
            .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static ListTargetsByRuleRequest translateToListTargetsByRuleRequest(final ResourceModel model) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return ListTargetsByRuleRequest.builder()
            .rule(model.getName())
            .eventBusName(model.getEventBusName())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel.ResourceModelBuilder translateFromReadResponse(final DescribeRuleResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
    HashMap<String, Object> eventPattern;

    try {
      eventPattern = MAPPER.readValue(
            awsResponse.eventPattern(),
            typeRef
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return ResourceModel.builder()
            .arn(awsResponse.arn())
            .description(awsResponse.description())
            .eventBusName(awsResponse.eventBusName())
            .eventPattern(eventPattern)
            .name(awsResponse.name())
            .roleArn(awsResponse.roleArn())
            .scheduleExpression(awsResponse.scheduleExpression())
            .state(awsResponse.stateAsString());
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static Set<software.amazon.events.rule.Target> translateFromResponseToTargets(final ListTargetsByRuleResponse awsResponse) {
    Set<software.amazon.events.rule.Target> targets = new HashSet<>();

    if (awsResponse.targets() != null) {

      for (software.amazon.awssdk.services.cloudwatchevents.model.Target target : awsResponse.targets()) {
        software.amazon.events.rule.Target thisTarget = new software.amazon.events.rule.Target();

        Set<software.amazon.events.rule.RunCommandTarget> runCommandTargets = new HashSet<>();

        software.amazon.events.rule.BatchParameters.BatchParametersBuilder batchParametersBuilder = new software.amazon.events.rule.BatchParameters.BatchParametersBuilder();
        software.amazon.events.rule.BatchRetryStrategy.BatchRetryStrategyBuilder batchRetryStrategyBuilder = new software.amazon.events.rule.BatchRetryStrategy.BatchRetryStrategyBuilder();
        software.amazon.events.rule.BatchArrayProperties.BatchArrayPropertiesBuilder batchArrayPropertiesBuilder = new software.amazon.events.rule.BatchArrayProperties.BatchArrayPropertiesBuilder();
        software.amazon.events.rule.DeadLetterConfig.DeadLetterConfigBuilder deadLetterConfigBuilder = new software.amazon.events.rule.DeadLetterConfig.DeadLetterConfigBuilder();
        software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder = new software.amazon.events.rule.EcsParameters.EcsParametersBuilder();
        software.amazon.events.rule.NetworkConfiguration.NetworkConfigurationBuilder networkConfigurationBuilder = new software.amazon.events.rule.NetworkConfiguration.NetworkConfigurationBuilder();
        software.amazon.events.rule.AwsVpcConfiguration.AwsVpcConfigurationBuilder awsVpcConfigurationBuilder = new software.amazon.events.rule.AwsVpcConfiguration.AwsVpcConfigurationBuilder();
        software.amazon.events.rule.HttpParameters.HttpParametersBuilder httpParametersBuilder = new software.amazon.events.rule.HttpParameters.HttpParametersBuilder();
        software.amazon.events.rule.InputTransformer.InputTransformerBuilder inputTransformerBuilder = new software.amazon.events.rule.InputTransformer.InputTransformerBuilder();
        software.amazon.events.rule.KinesisParameters.KinesisParametersBuilder kinesisParametersBuilder = new software.amazon.events.rule.KinesisParameters.KinesisParametersBuilder();
        software.amazon.events.rule.RedshiftDataParameters.RedshiftDataParametersBuilder redshiftDataParametersBuilder = new software.amazon.events.rule.RedshiftDataParameters.RedshiftDataParametersBuilder();
        software.amazon.events.rule.RetryPolicy.RetryPolicyBuilder retryPolicyBuilder = new software.amazon.events.rule.RetryPolicy.RetryPolicyBuilder();
        software.amazon.events.rule.RunCommandParameters.RunCommandParametersBuilder runCommandParametersBuilder = new software.amazon.events.rule.RunCommandParameters.RunCommandParametersBuilder();
        software.amazon.events.rule.SqsParameters.SqsParametersBuilder sqsParametersBuilder = new software.amazon.events.rule.SqsParameters.SqsParametersBuilder();

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
            runCommandTargets.add(new software.amazon.events.rule.RunCommandTarget.RunCommandTargetBuilder()
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
    }
    return targets;
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteRuleRequest translateToDeleteRuleRequest(final ResourceModel model) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    return DeleteRuleRequest.builder()
            .name(model.getName())
            .eventBusName(model.getEventBusName())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static RemoveTargetsRequest translateToRemoveTargetsRequest(final ResourceModel model, ListTargetsByRuleResponse listTargetsByRuleResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    ArrayList<String> targetIds = new ArrayList<>();
    for (Target target : listTargetsByRuleResponse.targets()) {
      targetIds.add(target.id());
    }

    return translateToRemoveTargetsRequest(model, targetIds);
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static RemoveTargetsRequest translateToRemoveTargetsRequest(final ResourceModel model, Collection<String> targetIds) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    return RemoveTargetsRequest.builder()
            .rule(model.getName())
            .eventBusName(model.getEventBusName())
            .ids(targetIds)
            .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListRulesRequest translateToListRulesRequest(final String nextToken) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L26-L31
    return ListRulesRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRulesResponse(final ListRulesResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(awsResponse.rules())
        .map(resource -> ResourceModel.builder()
                .name(resource.name())
                .eventBusName(resource.eventBusName())
            // include only primary identifier
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }

  private static List<Tag> translateModelTagsMapToSdk(
          Map<String, String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }

    return streamOfOrEmpty(tags.entrySet())
            .map(e -> Tag.builder()
                    .key(e.getKey())
                    .value(e.getValue())
                    .build())
            .collect(Collectors.toList());
  }
}
