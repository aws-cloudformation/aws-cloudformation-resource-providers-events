package software.amazon.events.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
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

  private static final ObjectMapper objectMapper = new ObjectMapper();

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
      eventPattern = objectMapper.writeValueAsString(model.getEventPattern());
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
  static ResourceModel translateFromReadResponse(final DescribeRuleResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
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

    return ResourceModel.builder()
            .arn(awsResponse.arn())
            .description(awsResponse.description())
            .eventBusName(awsResponse.eventBusName())
            .eventPattern(eventPattern)
            .name(awsResponse.name())
            .roleArn(awsResponse.roleArn())
            .scheduleExpression(awsResponse.scheduleExpression())
            .state(awsResponse.stateAsString())
        .build();
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
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToFirstUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L45-L50
    return awsRequest;
  }

  /**
   * Request to update some other properties that could not be provisioned through first update request
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToSecondUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    return awsRequest;
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
