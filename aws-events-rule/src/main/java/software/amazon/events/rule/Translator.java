package software.amazon.events.rule;

import com.fasterxml.jackson.core.JsonProcessingException;

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
import software.amazon.awssdk.services.cloudwatchevents.model.PlacementStrategy;
import software.amazon.awssdk.services.cloudwatchevents.model.RedshiftDataParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.RetryPolicy;
import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.SageMakerPipelineParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.SqsParameters;
import software.amazon.awssdk.services.cloudwatchevents.model.Target;
import software.amazon.awssdk.services.cloudwatchevents.model.BatchArrayProperties;
import software.amazon.awssdk.services.cloudwatchevents.model.RunCommandTarget;
import software.amazon.awssdk.services.cloudwatchevents.model.PutRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.PutTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.DescribeRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.DeleteRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.RemoveTargetsRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListRulesRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListTargetsByRuleRequest;
import software.amazon.awssdk.services.cloudwatchevents.model.ListRulesResponse;
import software.amazon.awssdk.services.cloudwatchevents.model.CapacityProviderStrategyItem;
import software.amazon.awssdk.services.cloudwatchevents.model.PlacementConstraint;
import software.amazon.awssdk.services.cloudwatchevents.model.Tag;
import software.amazon.awssdk.services.cloudwatchevents.model.SageMakerPipelineParameter;
import software.amazon.cloudformation.exceptions.TerminalException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Translator {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // CREATE/UPDATE

  /**
   * Generates a PutRuleRequest based on a ResourceModel.
   * @param model A ResourceModel containing data to make a PutRuleRequest
   * @param tags Unused
   * @return A PutRuleRequest
   */
  static PutRuleRequest translateToPutRuleRequest(final ResourceModel model, Map<String, String> tags, final CompositePID compositePID) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    String eventPattern = null;
    PutRuleRequest.Builder putRuleRequestBuilder = PutRuleRequest.builder();

    if (model.getEventPattern() != null) {
      try {
        eventPattern = MAPPER.writeValueAsString(model.getEventPattern());
      } catch (final JsonProcessingException e) {
        throw new TerminalException(e);
      }
    }

    return putRuleRequestBuilder
            .name(compositePID.getEventRuleName())
            .eventBusName(compositePID.getEventBusName())

            .description(model.getDescription())
            .eventPattern(eventPattern)
            .roleArn(model.getRoleArn())
            .scheduleExpression(model.getScheduleExpression())
            .state(model.getState())
            .build();
  }

  /**
   * Generates a PutTargetsRequest based on a ResourceModel.
   * @param model A ResourceModel containing data to make a PutTargetsRequest
   * @return A PutTargetsRequest
   */
  static PutTargetsRequest translateToPutTargetsRequest(final ResourceModel model, final CompositePID compositePID) {
    PutTargetsRequest putTargetsRequest = null;

    if (model.getTargets() != null) {

      ArrayList<Target> targets = new ArrayList<>();

      for (software.amazon.events.rule.Target target : model.getTargets()) {
        Target.Builder targetBuilder = Target.builder();

        addBatchParameters(targetBuilder, target.getBatchParameters());
        addDeadLetterConfig(targetBuilder, target.getDeadLetterConfig());
        addEcsParameters(targetBuilder, target.getEcsParameters());
        addHttpParameters(targetBuilder, target.getHttpParameters());
        addInputTransformer(targetBuilder, target.getInputTransformer());
        addKinesisParameters(targetBuilder, target.getKinesisParameters());
        addRedshiftDataParameters(targetBuilder, target.getRedshiftDataParameters());
        addRetryPolicy(targetBuilder, target.getRetryPolicy());
        addRunCommandParameters(targetBuilder, target.getRunCommandParameters());
        addSqsParameters(targetBuilder, target.getSqsParameters());
        addSageMakerPipelineParameters(targetBuilder, target.getSageMakerPipelineParameters());

        targets.add(targetBuilder
                .arn(target.getArn())
                .id(target.getId())
                .input(target.getInput())
                .inputPath(target.getInputPath())
                .roleArn(target.getRoleArn())
                .build()
        );
      }

      putTargetsRequest = PutTargetsRequest.builder()
              .rule(compositePID.getEventRuleName())
              .eventBusName(compositePID.getEventBusName())
              .targets(targets)
              .build();
    }

    return putTargetsRequest;
  }

  // REA
  static DescribeRuleRequest translateToDescribeRuleRequest(final CompositePID compositePID) {

    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return DescribeRuleRequest.builder()
            .name(compositePID.getEventRuleName())
            .eventBusName(compositePID.getEventBusName())
            .build();
  }

  /**
   * Generates a ResourceModelBuilder based on a DescribeRuleResponse with all data excluding Target data.
   * The resulting ResourceModelBuilder may be used with the targets returned from
   * translateFromListTargetsByRuleResponse to create a complete ResourceModel.
   * @param awsResponse A DescribeRuleResponse
   * @return A ResourceModelBuilder
   */
  static ResourceModel.ResourceModelBuilder translateFromDescribeRuleResponse(final DescribeRuleResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L58-L73
    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
    HashMap<String, Object> eventPattern = null;

    if (awsResponse.eventPattern() != null) {
      try {
        eventPattern = MAPPER.readValue(
                awsResponse.eventPattern(),
                typeRef
        );
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
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
   * Generates a Set of Targets based on a ListTargetsByRuleResponse.
   * @param awsResponse A ListTargetsByRuleResponse
   * @return A set of Targets
   */
  static Set<software.amazon.events.rule.Target> translateFromListTargetsByRuleResponse(final ListTargetsByRuleResponse awsResponse) {
    Set<software.amazon.events.rule.Target> targets = new HashSet<>();

    if (awsResponse.targets() != null) {

      for (Target target : awsResponse.targets()) {

        software.amazon.events.rule.Target.TargetBuilder targetBuilder = software.amazon.events.rule.Target.builder();

        addBatchParameters(target, targetBuilder, target.batchParameters());
        addDeadLetterConfig(targetBuilder, target.deadLetterConfig());
        addEcsParameters(target, targetBuilder, target.ecsParameters());
        addHttpParameters(targetBuilder, target.httpParameters());
        addInputTransformer(targetBuilder, target.inputTransformer());
        addKinesisParameters(targetBuilder, target.kinesisParameters());
        addRedshiftDataParameters(targetBuilder, target.redshiftDataParameters());
        addRetryPolicy(targetBuilder, target.retryPolicy());
        addRunCommandParameters(targetBuilder, target.runCommandParameters());
        addSqsParameters(targetBuilder, target.sqsParameters());
        addSageMakerPipelineParameters(targetBuilder, target.sageMakerPipelineParameters());

        targets.add(targetBuilder
                .arn(target.arn())
                .id(target.id())
                .input(target.input())
                .inputPath(target.inputPath())
                .roleArn(target.roleArn())
                .build()
        );
      }
    }
    return targets;
  }

  // DELETE
  static DeleteRuleRequest translateToDeleteRuleRequest(final CompositePID compositePID) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    return DeleteRuleRequest.builder()
            .name(compositePID.getEventRuleName())
            .eventBusName(compositePID.getEventBusName())
            .build();
  }

  static RemoveTargetsRequest translateToRemoveTargetsRequest(final CompositePID compositePID, Collection<String> targetIds) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    return RemoveTargetsRequest.builder()
            .rule(compositePID.getEventRuleName())
            .eventBusName(compositePID.getEventBusName())
            .ids(targetIds)
            .build();
  }

  static RemoveTargetsRequest translateToRemoveTargetsRequest(final CompositePID compositePID, ListTargetsByRuleResponse listTargetsByRuleResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    ArrayList<String> targetIds = new ArrayList<>();
    for (Target target : listTargetsByRuleResponse.targets()) {
      targetIds.add(target.id());
    }

    return translateToRemoveTargetsRequest(compositePID, targetIds);
  }

  // LIST

  /**
   * Generates a ListRulesRequest.
   * @param nextToken The nextToken in case there are too many Rules to be sent in one SDK call
   * @return A ListRulesRequest
   */
  static ListRulesRequest translateToListRulesRequest(final String nextToken) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L26-L31
    return ListRulesRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  static ListTargetsByRuleRequest translateToListTargetsByRuleRequest(final CompositePID compositePID) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return ListTargetsByRuleRequest.builder()
            .rule(compositePID.getEventRuleName())
            .eventBusName(compositePID.getEventBusName())
            .build();
  }

  /**
   * Generates a list of ResourceModels each containing a RuleId based on a ListRulesResponse.
   * @param awsResponse A ListRulesResponse
   * @return A List of ResourceModels
   */
  static List<ResourceModel> translateFromListRulesResponse(final ListRulesResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(awsResponse.rules())
        .map(resource -> ResourceModel.builder()
                .arn(resource.arn())
            // include only primary identifier
            .build())
        .collect(Collectors.toList());
  }

  // STATIC HELPER FUNCTIONS

  private static void addSageMakerPipelineParameters(Target.Builder targetBuilder, software.amazon.events.rule.SageMakerPipelineParameters sageMakerPipelineParameters) {
    if (sageMakerPipelineParameters != null &&
            sageMakerPipelineParameters.getPipelineParameterList() != null) {
      ArrayList<SageMakerPipelineParameter> pipelineParameterList = new ArrayList<>();

      for (software.amazon.events.rule.SageMakerPipelineParameter sageMakerPipelineParameter : sageMakerPipelineParameters.getPipelineParameterList()) {
        pipelineParameterList.add(SageMakerPipelineParameter.builder()
                .name(sageMakerPipelineParameter.getName())
                .value(sageMakerPipelineParameter.getValue())
                .build()
        );
      }

      targetBuilder.sageMakerPipelineParameters(SageMakerPipelineParameters.builder()
              .pipelineParameterList(pipelineParameterList)
              .build()
      );
    }
  }

  private static void addSqsParameters(Target.Builder targetBuilder, software.amazon.events.rule.SqsParameters sqsParameters) {
    if (sqsParameters != null) {
      targetBuilder.sqsParameters(SqsParameters.builder()
              .messageGroupId(sqsParameters.getMessageGroupId())
              .build());
    }
  }

  private static void addRunCommandParameters(Target.Builder targetBuilder, software.amazon.events.rule.RunCommandParameters runCommandParameters) {
    if (runCommandParameters != null && runCommandParameters.getRunCommandTargets() != null) {
      ArrayList<RunCommandTarget> runCommandTargets = new ArrayList<>();

      for (software.amazon.events.rule.RunCommandTarget value : runCommandParameters.getRunCommandTargets()) {
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
  }

  private static void addRetryPolicy(Target.Builder targetBuilder, software.amazon.events.rule.RetryPolicy retryPolicy) {
    if (retryPolicy != null) {
      targetBuilder.retryPolicy(RetryPolicy.builder()
              .maximumEventAgeInSeconds(retryPolicy.getMaximumEventAgeInSeconds())
              .maximumRetryAttempts(retryPolicy.getMaximumRetryAttempts())
              .build());
    }
  }

  private static void addRedshiftDataParameters(Target.Builder targetBuilder, software.amazon.events.rule.RedshiftDataParameters redshiftDataParameters) {
    if (redshiftDataParameters != null) {
      targetBuilder.redshiftDataParameters(RedshiftDataParameters.builder()
              .database(redshiftDataParameters.getDatabase())
              .dbUser(redshiftDataParameters.getDbUser())
              .secretManagerArn(redshiftDataParameters.getSecretManagerArn())
              .sql(redshiftDataParameters.getSql())
              .statementName(redshiftDataParameters.getStatementName())
              .withEvent(redshiftDataParameters.getWithEvent())
              .build());
    }
  }

  private static void addKinesisParameters(Target.Builder targetBuilder, software.amazon.events.rule.KinesisParameters kinesisParameters) {
    if (kinesisParameters != null) {
      targetBuilder.kinesisParameters(KinesisParameters.builder()
              .partitionKeyPath(kinesisParameters.getPartitionKeyPath())
              .build());
    }
  }

  private static void addInputTransformer(Target.Builder targetBuilder, software.amazon.events.rule.InputTransformer inputTransformer) {
    if (inputTransformer != null) {
      targetBuilder.inputTransformer(InputTransformer.builder()
              .inputPathsMap(inputTransformer.getInputPathsMap())
              .inputTemplate(inputTransformer.getInputTemplate())
              .build());
    }
  }

  private static void addHttpParameters(Target.Builder targetBuilder, software.amazon.events.rule.HttpParameters httpParameters) {
    if (httpParameters != null) {
      targetBuilder.httpParameters(HttpParameters.builder()
              .headerParameters(httpParameters.getHeaderParameters())
              .pathParameterValues(httpParameters.getPathParameterValues())
              .queryStringParameters(httpParameters.getQueryStringParameters())
              .build());
    }
  }

  private static void addEcsParameters(Target.Builder targetBuilder, software.amazon.events.rule.EcsParameters ecsParameters) {
    if (ecsParameters != null) {
      EcsParameters.Builder ecsParametersBuilder = EcsParameters.builder();

      addNetworkConfiguration(ecsParametersBuilder, ecsParameters.getNetworkConfiguration());
      addCapacityProviderStrategy(ecsParametersBuilder, ecsParameters.getCapacityProviderStrategy());
      addPlacementConstraints(ecsParametersBuilder, ecsParameters.getPlacementConstraints());
      addPlacementStrategies(ecsParametersBuilder, ecsParameters.getPlacementStrategies());
      addTagList(ecsParametersBuilder, ecsParameters.getTagList());

      targetBuilder.ecsParameters(ecsParametersBuilder
              .enableECSManagedTags(ecsParameters.getEnableECSManagedTags())
              .enableExecuteCommand(ecsParameters.getEnableExecuteCommand())
              .group(ecsParameters.getGroup())
              .launchType(ecsParameters.getLaunchType())
              .platformVersion(ecsParameters.getPlatformVersion())
              .propagateTags(ecsParameters.getPropagateTags())
              .referenceId(ecsParameters.getReferenceId())
              .taskCount(ecsParameters.getTaskCount())
              .taskDefinitionArn(ecsParameters.getTaskDefinitionArn())
              .build());
    }
  }

  private static void addTagList(EcsParameters.Builder ecsParametersBuilder, Set<software.amazon.events.rule.Tag> tagList) {
    if (tagList != null) {
      ArrayList<Tag> tags = new ArrayList<>();

      for (software.amazon.events.rule.Tag tag : tagList) {
        tags.add(Tag.builder()
                .key(tag.getKey())
                .value(tag.getValue())
                .build()
        );
      }

      ecsParametersBuilder.tags(tags);
    }
  }

  private static void addPlacementStrategies(EcsParameters.Builder ecsParametersBuilder, Set<software.amazon.events.rule.PlacementStrategy> placementStrategies) {
    if (placementStrategies != null) {
      ArrayList<PlacementStrategy> placementStrategyList = new ArrayList<>();

      for (software.amazon.events.rule.PlacementStrategy placementStrategy : placementStrategies) {
        placementStrategyList.add(PlacementStrategy.builder()
                .field(placementStrategy.getField())
                .type(placementStrategy.getType())
                .build()
        );
      }

      ecsParametersBuilder.placementStrategy(placementStrategyList);
    }
  }

  private static void addPlacementConstraints(EcsParameters.Builder ecsParametersBuilder, Set<software.amazon.events.rule.PlacementConstraint> placementConstraints) {
    if (placementConstraints != null) {
      ArrayList<PlacementConstraint> placementConstraintList = new ArrayList<>();

      for (software.amazon.events.rule.PlacementConstraint placementConstraint : placementConstraints) {
        placementConstraintList.add(PlacementConstraint.builder()
                .expression(placementConstraint.getExpression())
                .type(placementConstraint.getType())
                .build()
        );
      }

      ecsParametersBuilder.placementConstraints(placementConstraintList);
    }
  }

  private static void addCapacityProviderStrategy(EcsParameters.Builder ecsParametersBuilder, Set<software.amazon.events.rule.CapacityProviderStrategyItem> capacityProviderStrategy) {
    if (capacityProviderStrategy != null) {
      ArrayList<CapacityProviderStrategyItem> capacityProviderStrategyItems = new ArrayList<>();

      for (software.amazon.events.rule.CapacityProviderStrategyItem capacityProviderStrategyItem : capacityProviderStrategy) {
        capacityProviderStrategyItems.add(CapacityProviderStrategyItem.builder()
                .base(capacityProviderStrategyItem.getBase())
                .capacityProvider(capacityProviderStrategyItem.getCapacityProvider())
                .weight(capacityProviderStrategyItem.getWeight())
                .build()
        );
      }

      ecsParametersBuilder.capacityProviderStrategy(capacityProviderStrategyItems);
    }
  }

  private static void addNetworkConfiguration(EcsParameters.Builder ecsParametersBuilder, software.amazon.events.rule.NetworkConfiguration networkConfiguration) {
    if (networkConfiguration != null &&
            networkConfiguration.getAwsVpcConfiguration() != null) {
      ecsParametersBuilder.networkConfiguration(NetworkConfiguration.builder()
              .awsvpcConfiguration(AwsVpcConfiguration.builder()
                      .assignPublicIp(networkConfiguration.getAwsVpcConfiguration().getAssignPublicIp())
                      .securityGroups(networkConfiguration.getAwsVpcConfiguration().getSecurityGroups())
                      .subnets(networkConfiguration.getAwsVpcConfiguration().getSubnets())
                      .build())
              .build());
    }
  }

  private static void addDeadLetterConfig(Target.Builder targetBuilder, software.amazon.events.rule.DeadLetterConfig deadLetterConfig) {
    if (deadLetterConfig != null) {
      targetBuilder.deadLetterConfig(DeadLetterConfig.builder()
              .arn(deadLetterConfig.getArn())
              .build());
    }
  }

  private static void addBatchParameters(Target.Builder targetBuilder, software.amazon.events.rule.BatchParameters batchParameters) {
    if (batchParameters != null) {
      BatchParameters.Builder batchParametersBuilder = BatchParameters.builder();

      if (batchParameters.getArrayProperties() != null) {
        batchParametersBuilder.arrayProperties(BatchArrayProperties.builder()
                .size(batchParameters.getArrayProperties().getSize())
                .build());
      }

      if (batchParameters.getRetryStrategy() != null) {
        batchParametersBuilder.retryStrategy(BatchRetryStrategy.builder()
                .attempts(batchParameters.getRetryStrategy().getAttempts())
                .build());
      }

      targetBuilder.batchParameters(batchParametersBuilder
              .jobDefinition(batchParameters.getJobDefinition())
              .jobName(batchParameters.getJobName())
              .build());
    }
  }

  private static void addSageMakerPipelineParameters(software.amazon.events.rule.Target.TargetBuilder targetBuilder, SageMakerPipelineParameters sageMakerPipelineParameters) {
    if (sageMakerPipelineParameters != null &&
            sageMakerPipelineParameters.pipelineParameterList() != null &&
            !sageMakerPipelineParameters.pipelineParameterList().isEmpty()) {
      HashSet<software.amazon.events.rule.SageMakerPipelineParameter> pipelineParameterList = new HashSet<>();

      for (SageMakerPipelineParameter sageMakerPipelineParameter : sageMakerPipelineParameters.pipelineParameterList()) {
        pipelineParameterList.add(software.amazon.events.rule.SageMakerPipelineParameter.builder()
                .name(sageMakerPipelineParameter.name())
                .value(sageMakerPipelineParameter.value())
                .build()
        );
      }

      targetBuilder.sageMakerPipelineParameters(software.amazon.events.rule.SageMakerPipelineParameters.builder()
              .pipelineParameterList(pipelineParameterList)
              .build()
      );
    }
  }

  private static void addSqsParameters(software.amazon.events.rule.Target.TargetBuilder targetBuilder, SqsParameters sqsParameters) {
    if (sqsParameters != null) {
      targetBuilder.sqsParameters(software.amazon.events.rule.SqsParameters.builder()
              .messageGroupId(sqsParameters.messageGroupId())
              .build());
    }
  }

  private static void addRunCommandParameters(software.amazon.events.rule.Target.TargetBuilder targetBuilder, RunCommandParameters runCommandParameters) {
    if (runCommandParameters != null && runCommandParameters.runCommandTargets() != null) {
      HashSet<software.amazon.events.rule.RunCommandTarget> runCommandTargets = new HashSet<>();

      for (RunCommandTarget value : runCommandParameters.runCommandTargets()) {
        runCommandTargets.add(software.amazon.events.rule.RunCommandTarget.builder()
                .key(value.key())
                .values(new HashSet<>(value.values()))
                .build()
        );
      }

      targetBuilder.runCommandParameters(software.amazon.events.rule.RunCommandParameters.builder()
              .runCommandTargets(runCommandTargets)
              .build());
    }
  }

  private static void addRetryPolicy(software.amazon.events.rule.Target.TargetBuilder targetBuilder, RetryPolicy retryPolicy) {
    if (retryPolicy != null) {
      targetBuilder.retryPolicy(software.amazon.events.rule.RetryPolicy.builder()
              .maximumEventAgeInSeconds(retryPolicy.maximumEventAgeInSeconds())
              .maximumRetryAttempts(retryPolicy.maximumRetryAttempts())
              .build());
    }
  }

  private static void addRedshiftDataParameters(software.amazon.events.rule.Target.TargetBuilder targetBuilder, RedshiftDataParameters redshiftDataParameters) {
    if (redshiftDataParameters != null) {
      targetBuilder.redshiftDataParameters(software.amazon.events.rule.RedshiftDataParameters.builder()
              .database(redshiftDataParameters.database())
              .dbUser(redshiftDataParameters.dbUser())
              .secretManagerArn(redshiftDataParameters.secretManagerArn())
              .sql(redshiftDataParameters.sql())
              .statementName(redshiftDataParameters.statementName())
              .withEvent(redshiftDataParameters.withEvent())
              .build());
    }
  }

  private static void addKinesisParameters(software.amazon.events.rule.Target.TargetBuilder targetBuilder, KinesisParameters kinesisParameters) {
    if (kinesisParameters != null) {
      targetBuilder.kinesisParameters(software.amazon.events.rule.KinesisParameters.builder()
              .partitionKeyPath(kinesisParameters.partitionKeyPath())
              .build());
    }
  }

  private static void addInputTransformer(software.amazon.events.rule.Target.TargetBuilder targetBuilder, InputTransformer inputTransformer) {
    if (inputTransformer != null) {
      targetBuilder.inputTransformer(software.amazon.events.rule.InputTransformer.builder()
              .inputPathsMap(inputTransformer.inputPathsMap())
              .inputTemplate(inputTransformer.inputTemplate())
              .build());
    }
  }

  private static void addHttpParameters(software.amazon.events.rule.Target.TargetBuilder targetBuilder, HttpParameters httpParameters) {
    if (httpParameters != null) {
      targetBuilder.httpParameters(software.amazon.events.rule.HttpParameters.builder()
              .headerParameters(httpParameters.headerParameters())
              .pathParameterValues(new HashSet<>(httpParameters.pathParameterValues()))
              .queryStringParameters(httpParameters.queryStringParameters())
              .build());
    }
  }

  private static void addEcsParameters(Target target, software.amazon.events.rule.Target.TargetBuilder targetBuilder, EcsParameters ecsParameters) {
    if (ecsParameters != null) {
      software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder = software.amazon.events.rule.EcsParameters.builder();

      addNetworkConfiguration(ecsParametersBuilder, target.ecsParameters().networkConfiguration());
      addCapacityProviderStrategy(ecsParametersBuilder, target.ecsParameters().capacityProviderStrategy());
      addPlacementConstraints(ecsParametersBuilder, target.ecsParameters().placementConstraints());
      addPlacementStrategy(ecsParametersBuilder, target.ecsParameters().placementStrategy());
      addTags(ecsParametersBuilder, target.ecsParameters().tags());

      targetBuilder.ecsParameters(ecsParametersBuilder
              .enableECSManagedTags(ecsParameters.enableECSManagedTags())
              .enableExecuteCommand(ecsParameters.enableExecuteCommand())
              .group(ecsParameters.group())
              .launchType(ecsParameters.launchTypeAsString())
              .platformVersion(ecsParameters.platformVersion())
              .propagateTags(ecsParameters.propagateTagsAsString())
              .referenceId(ecsParameters.referenceId())
              .taskCount(ecsParameters.taskCount())
              .taskDefinitionArn(ecsParameters.taskDefinitionArn())
              .build());
    }
  }

  private static void addTags(software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder, List<Tag> tags) {
    if (tags != null &&
            !tags.isEmpty()) {
      HashSet<software.amazon.events.rule.Tag> tagList = new HashSet<>();

      for (Tag tag : tags) {
        tagList.add(software.amazon.events.rule.Tag.builder()
                .key(tag.key())
                .value(tag.value())
                .build()
        );
      }

      ecsParametersBuilder.tagList(tagList);
    }
  }

  private static void addPlacementStrategy(software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder, List<PlacementStrategy> placementStrategies) {
    if (placementStrategies != null &&
            !placementStrategies.isEmpty()) {
      HashSet<software.amazon.events.rule.PlacementStrategy> placementStrategyList = new HashSet<>();

      for (PlacementStrategy placementStrategy : placementStrategies) {
        placementStrategyList.add(software.amazon.events.rule.PlacementStrategy.builder()
                .field(placementStrategy.field())
                .type(placementStrategy.typeAsString())
                .build()
        );
      }

      ecsParametersBuilder.placementStrategies(placementStrategyList);
    }
  }

  private static void addPlacementConstraints(software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder, List<PlacementConstraint> placementConstraints) {
    if (placementConstraints != null &&
            !placementConstraints.isEmpty()) {
      HashSet<software.amazon.events.rule.PlacementConstraint> placementConstraintList = new HashSet<>();

      for (PlacementConstraint placementConstraint : placementConstraints) {
        placementConstraintList.add(software.amazon.events.rule.PlacementConstraint.builder()
                .expression(placementConstraint.expression())
                .type(placementConstraint.typeAsString())
                .build()
        );
      }

      ecsParametersBuilder.placementConstraints(placementConstraintList);
    }
  }

  private static void addCapacityProviderStrategy(software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder, List<CapacityProviderStrategyItem> capacityProviderStrategyItems) {
    if (capacityProviderStrategyItems != null &&
            !capacityProviderStrategyItems.isEmpty()) {
      HashSet<software.amazon.events.rule.CapacityProviderStrategyItem> capacityProviderStrategyItemList = new HashSet<>();

      for (CapacityProviderStrategyItem capacityProviderStrategyItem : capacityProviderStrategyItems) {
        capacityProviderStrategyItemList.add(software.amazon.events.rule.CapacityProviderStrategyItem.builder()
                .base(capacityProviderStrategyItem.base())
                .capacityProvider(capacityProviderStrategyItem.capacityProvider())
                .weight(capacityProviderStrategyItem.weight())
                .build()
        );
      }

      ecsParametersBuilder.capacityProviderStrategy(capacityProviderStrategyItemList);
    }
  }

  private static void addNetworkConfiguration(software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParametersBuilder, NetworkConfiguration networkConfiguration) {
    if (networkConfiguration != null &&
            networkConfiguration.awsvpcConfiguration() != null) {
      ecsParametersBuilder.networkConfiguration(software.amazon.events.rule.NetworkConfiguration.builder()
              .awsVpcConfiguration(software.amazon.events.rule.AwsVpcConfiguration.builder()
                      .assignPublicIp(networkConfiguration.awsvpcConfiguration().assignPublicIp().name())
                      .securityGroups(new HashSet<>(networkConfiguration.awsvpcConfiguration().securityGroups()))
                      .subnets(new HashSet<>(networkConfiguration.awsvpcConfiguration().subnets()))
                      .build())
              .build());
    }
  }

  private static void addDeadLetterConfig(software.amazon.events.rule.Target.TargetBuilder targetBuilder, DeadLetterConfig deadLetterConfig) {
    if (deadLetterConfig != null) {
      targetBuilder.deadLetterConfig(software.amazon.events.rule.DeadLetterConfig.builder()
              .arn(deadLetterConfig.arn())
              .build());
    }
  }

  private static void addBatchParameters(Target target, software.amazon.events.rule.Target.TargetBuilder targetBuilder, BatchParameters batchParameters) {
    if (batchParameters != null) {
      software.amazon.events.rule.BatchParameters.BatchParametersBuilder batchParametersBuilder = software.amazon.events.rule.BatchParameters.builder();

      addArrayProperties(batchParametersBuilder, target.batchParameters().arrayProperties());

      addRetryStrategy(batchParametersBuilder, target.batchParameters().retryStrategy());

      targetBuilder.batchParameters(batchParametersBuilder
              .jobDefinition(batchParameters.jobDefinition())
              .jobName(batchParameters.jobName())
              .build());
    }
  }

  private static void addRetryStrategy(software.amazon.events.rule.BatchParameters.BatchParametersBuilder batchParameters, BatchRetryStrategy batchRetryStrategy) {
    if (batchRetryStrategy != null) {
      batchParameters.retryStrategy(software.amazon.events.rule.BatchRetryStrategy.builder()
              .attempts(batchRetryStrategy.attempts())
              .build());
    }
  }

  private static void addArrayProperties(software.amazon.events.rule.BatchParameters.BatchParametersBuilder batchParametersBuilder, BatchArrayProperties batchArrayProperties) {
    if (batchArrayProperties != null) {
      batchParametersBuilder.arrayProperties(software.amazon.events.rule.BatchArrayProperties.builder()
              .size(batchArrayProperties.size())
              .build());
    }
  }

  // OTHER

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }
}
