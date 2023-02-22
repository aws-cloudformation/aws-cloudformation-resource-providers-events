package software.amazon.events.rule;

import com.fasterxml.jackson.core.JsonProcessingException;
import software.amazon.awssdk.awscore.AwsRequest;

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
import software.amazon.awssdk.services.cloudwatchevents.model.CloudWatchEventsRequest;
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

  static PutRuleRequest translateToPutRuleRequest(final ResourceModel model, Map<String, String> tags) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    String eventPattern = null;
    PutRuleRequest.Builder putRuleRequest = PutRuleRequest.builder();
    CompositeId compositeId = new CompositeId(model);

    if (model.getEventPattern() != null) {
      try {
        eventPattern = MAPPER.writeValueAsString(model.getEventPattern());
      } catch (final JsonProcessingException e) {
        throw new TerminalException(e);
      }
    }

    return putRuleRequest
            .name(compositeId.ruleName)
            .eventBusName(compositeId.eventBusName)
            .description(model.getDescription())
            .eventPattern(eventPattern)
            .roleArn(model.getRoleArn())
            .scheduleExpression(model.getScheduleExpression())
            .state(model.getState())
            .build();
  }

  static PutTargetsRequest translateToPutTargetsRequest(final ResourceModel model) {
    CompositeId compositeId = new CompositeId(model);

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

        if (target.getEcsParameters().getCapacityProviderStrategy() != null) {
          ArrayList<CapacityProviderStrategyItem> capacityProviderStrategyItems = new ArrayList<>();

          for (software.amazon.events.rule.CapacityProviderStrategyItem capacityProviderStrategyItem : target.getEcsParameters().getCapacityProviderStrategy()) {
            capacityProviderStrategyItems.add(CapacityProviderStrategyItem.builder()
                    .base(capacityProviderStrategyItem.getBase())
                    .capacityProvider(capacityProviderStrategyItem.getCapacityProvider())
                    .weight(capacityProviderStrategyItem.getWeight())
                    .build()
            );
          }

          ecsParameters.capacityProviderStrategy(capacityProviderStrategyItems);
        }

        if (target.getEcsParameters().getPlacementConstraints() != null) {
          ArrayList<PlacementConstraint> placementConstraints = new ArrayList<>();

          for (software.amazon.events.rule.PlacementConstraint placementConstraint : target.getEcsParameters().getPlacementConstraints()) {
            placementConstraints.add(PlacementConstraint.builder()
                    .expression(placementConstraint.getExpression())
                    .type(placementConstraint.getType())
                    .build()
            );
          }

          ecsParameters.placementConstraints(placementConstraints);
        }

        if (target.getEcsParameters().getPlacementStrategies() != null) {
          ArrayList<PlacementStrategy> placementStrategies = new ArrayList<>();

          for (software.amazon.events.rule.PlacementStrategy placementStrategy : target.getEcsParameters().getPlacementStrategies()) {
            placementStrategies.add(PlacementStrategy.builder()
                    .field(placementStrategy.getField())
                    .type(placementStrategy.getType())
                    .build()
            );
          }

          ecsParameters.placementStrategy(placementStrategies);
        }

        if (target.getEcsParameters().getTagList() != null) {
          ArrayList<Tag> tags = new ArrayList<>();

          for (software.amazon.events.rule.Tag tag : target.getEcsParameters().getTagList()) {
            tags.add(Tag.builder()
                    .key(tag.getKey())
                    .value(tag.getValue())
                    .build()
            );
          }

          ecsParameters.tags(tags);
        }

        targetBuilder.ecsParameters(ecsParameters
                .enableECSManagedTags(target.getEcsParameters().getEnableECSManagedTags())
                .enableExecuteCommand(target.getEcsParameters().getEnableExecuteCommand())
                .group(target.getEcsParameters().getGroup())
                .launchType(target.getEcsParameters().getLaunchType())
                .platformVersion(target.getEcsParameters().getPlatformVersion())
                .propagateTags(target.getEcsParameters().getPropagateTags())
                .referenceId(target.getEcsParameters().getReferenceId())
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

      if (target.getSageMakerPipelineParameters() != null &&
              target.getSageMakerPipelineParameters().getPipelineParameterList() != null) {
        ArrayList<SageMakerPipelineParameter> sageMakerPipelineParameters = new ArrayList<>();

        for (software.amazon.events.rule.SageMakerPipelineParameter sageMakerPipelineParameter : target.getSageMakerPipelineParameters().getPipelineParameterList()) {
          sageMakerPipelineParameters.add(SageMakerPipelineParameter.builder()
                  .name(sageMakerPipelineParameter.getName())
                  .value(sageMakerPipelineParameter.getValue())
                  .build()
          );
        }

        targetBuilder.sageMakerPipelineParameters(SageMakerPipelineParameters.builder()
                .pipelineParameterList(sageMakerPipelineParameters)
                .build()
        );
      }

      targets.add(targetBuilder
              .arn(target.getArn())
              .id(target.getId())
              .input(target.getInput())
              .inputPath(target.getInputPath())
              .roleArn(target.getRoleArn())
              .build()
      );
    }

    return PutTargetsRequest.builder()
            .eventBusName(compositeId.eventBusName)
            .rule(compositeId.ruleName)
            .targets(targets)
            .build();
  }

  // READ

  static DescribeRuleRequest translateToDescribeRuleRequest(final ResourceModel model) {
    CompositeId compositeId = new CompositeId(model);

    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return DescribeRuleRequest.builder()
            .name(compositeId.ruleName)
            .eventBusName(compositeId.eventBusName)
            .build();
  }

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

  static Set<software.amazon.events.rule.Target> translateFromListTargetsByRuleResponse(final ListTargetsByRuleResponse awsResponse) {
    Set<software.amazon.events.rule.Target> targets = new HashSet<>();

    if (awsResponse.targets() != null) {

      for (Target target : awsResponse.targets()) {

        software.amazon.events.rule.Target.TargetBuilder targetBuilder = software.amazon.events.rule.Target.builder();

        if (target.batchParameters() != null) {
          software.amazon.events.rule.BatchParameters.BatchParametersBuilder batchParameters = software.amazon.events.rule.BatchParameters.builder();

          if (target.batchParameters().arrayProperties() != null) {
            batchParameters.arrayProperties(software.amazon.events.rule.BatchArrayProperties.builder()
                    .size(target.batchParameters().arrayProperties().size())
                    .build());
          }

          if (target.batchParameters().retryStrategy() != null) {
            batchParameters.retryStrategy(software.amazon.events.rule.BatchRetryStrategy.builder()
                    .attempts(target.batchParameters().retryStrategy().attempts())
                    .build());
          }

          targetBuilder.batchParameters(batchParameters
                  .jobDefinition(target.batchParameters().jobDefinition())
                  .jobName(target.batchParameters().jobName())
                  .build());
        }

        if (target.deadLetterConfig() != null) {
          targetBuilder.deadLetterConfig(software.amazon.events.rule.DeadLetterConfig.builder()
                  .arn(target.deadLetterConfig().arn())
                  .build());
        }

        if (target.ecsParameters() != null) {
          software.amazon.events.rule.EcsParameters.EcsParametersBuilder ecsParameters = software.amazon.events.rule.EcsParameters.builder();

          if (target.ecsParameters().networkConfiguration() != null &&
                  target.ecsParameters().networkConfiguration().awsvpcConfiguration() != null) {
            ecsParameters.networkConfiguration(software.amazon.events.rule.NetworkConfiguration.builder()
                    .awsVpcConfiguration(software.amazon.events.rule.AwsVpcConfiguration.builder()
                            .assignPublicIp(target.ecsParameters().networkConfiguration().awsvpcConfiguration().assignPublicIp().name())
                            .securityGroups(new HashSet<>(target.ecsParameters().networkConfiguration().awsvpcConfiguration().securityGroups()))
                            .subnets(new HashSet<>(target.ecsParameters().networkConfiguration().awsvpcConfiguration().subnets()))
                            .build())
                    .build());
          }

          if (target.ecsParameters().capacityProviderStrategy() != null &&
              !target.ecsParameters().capacityProviderStrategy().isEmpty()) {
            HashSet<software.amazon.events.rule.CapacityProviderStrategyItem> capacityProviderStrategyItems = new HashSet<>();

            for (CapacityProviderStrategyItem capacityProviderStrategyItem : target.ecsParameters().capacityProviderStrategy()) {
              capacityProviderStrategyItems.add(software.amazon.events.rule.CapacityProviderStrategyItem.builder()
                      .base(capacityProviderStrategyItem.base())
                      .capacityProvider(capacityProviderStrategyItem.capacityProvider())
                      .weight(capacityProviderStrategyItem.weight())
                      .build()
              );
            }

            ecsParameters.capacityProviderStrategy(capacityProviderStrategyItems);
          }

          if (target.ecsParameters().placementConstraints() != null &&
              !target.ecsParameters().placementConstraints().isEmpty()) {
            HashSet<software.amazon.events.rule.PlacementConstraint> placementConstraints = new HashSet<>();

            for (PlacementConstraint placementConstraint : target.ecsParameters().placementConstraints()) {
              placementConstraints.add(software.amazon.events.rule.PlacementConstraint.builder()
                      .expression(placementConstraint.expression())
                      .type(placementConstraint.typeAsString())
                      .build()
              );
            }

            ecsParameters.placementConstraints(placementConstraints);
          }

          if (target.ecsParameters().placementStrategy() != null &&
              !target.ecsParameters().placementStrategy().isEmpty()) {
            HashSet<software.amazon.events.rule.PlacementStrategy> placementStrategies = new HashSet<>();

            for (PlacementStrategy placementStrategy : target.ecsParameters().placementStrategy()) {
              placementStrategies.add(software.amazon.events.rule.PlacementStrategy.builder()
                      .field(placementStrategy.field())
                      .type(placementStrategy.typeAsString())
                      .build()
              );
            }

            ecsParameters.placementStrategies(placementStrategies);
          }

          if (target.ecsParameters().tags() != null &&
              !target.ecsParameters().tags().isEmpty()) {
            HashSet<software.amazon.events.rule.Tag> tags = new HashSet<>();

            for (Tag tag : target.ecsParameters().tags()) {
              tags.add(software.amazon.events.rule.Tag.builder()
                      .key(tag.key())
                      .value(tag.value())
                      .build()
              );
            }

            ecsParameters.tagList(tags);
          }

          targetBuilder.ecsParameters(ecsParameters
                  .enableECSManagedTags(target.ecsParameters().enableECSManagedTags())
                  .enableExecuteCommand(target.ecsParameters().enableExecuteCommand())
                  .group(target.ecsParameters().group())
                  .launchType(target.ecsParameters().launchTypeAsString())
                  .platformVersion(target.ecsParameters().platformVersion())
                  .propagateTags(target.ecsParameters().propagateTagsAsString())
                  .referenceId(target.ecsParameters().referenceId())
                  .taskCount(target.ecsParameters().taskCount())
                  .taskDefinitionArn(target.ecsParameters().taskDefinitionArn())
                  .build());
        }

        if (target.httpParameters() != null) {
          targetBuilder.httpParameters(software.amazon.events.rule.HttpParameters.builder()
                  .headerParameters(target.httpParameters().headerParameters())
                  .pathParameterValues(new HashSet<>(target.httpParameters().pathParameterValues()))
                  .queryStringParameters(target.httpParameters().queryStringParameters())
                  .build());
        }

        if (target.inputTransformer() != null) {
          targetBuilder.inputTransformer(software.amazon.events.rule.InputTransformer.builder()
                  .inputPathsMap(target.inputTransformer().inputPathsMap())
                  .inputTemplate(target.inputTransformer().inputTemplate())
                  .build());
        }

        if (target.kinesisParameters() != null) {
          targetBuilder.kinesisParameters(software.amazon.events.rule.KinesisParameters.builder()
                  .partitionKeyPath(target.kinesisParameters().partitionKeyPath())
                  .build());
        }

        if (target.redshiftDataParameters() != null) {
          targetBuilder.redshiftDataParameters(software.amazon.events.rule.RedshiftDataParameters.builder()
                  .database(target.redshiftDataParameters().database())
                  .dbUser(target.redshiftDataParameters().dbUser())
                  .secretManagerArn(target.redshiftDataParameters().secretManagerArn())
                  .sql(target.redshiftDataParameters().sql())
                  .statementName(target.redshiftDataParameters().statementName())
                  .withEvent(target.redshiftDataParameters().withEvent())
                  .build());
        }

        if (target.retryPolicy() != null) {
          targetBuilder.retryPolicy(software.amazon.events.rule.RetryPolicy.builder()
                  .maximumEventAgeInSeconds(target.retryPolicy().maximumEventAgeInSeconds())
                  .maximumRetryAttempts(target.retryPolicy().maximumRetryAttempts())
                  .build());
        }

        if (target.runCommandParameters() != null && target.runCommandParameters().runCommandTargets() != null) {
          HashSet<software.amazon.events.rule.RunCommandTarget> runCommandTargets = new HashSet<>();

          for (RunCommandTarget value : target.runCommandParameters().runCommandTargets()) {
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

        if (target.sqsParameters() != null) {
          targetBuilder.sqsParameters(software.amazon.events.rule.SqsParameters.builder()
                  .messageGroupId(target.sqsParameters().messageGroupId())
                  .build());
        }

        if (target.sageMakerPipelineParameters() != null &&
                target.sageMakerPipelineParameters().pipelineParameterList() != null &&
                !target.sageMakerPipelineParameters().pipelineParameterList().isEmpty()) {
          HashSet<software.amazon.events.rule.SageMakerPipelineParameter> sageMakerPipelineParameters = new HashSet<>();

          for (SageMakerPipelineParameter sageMakerPipelineParameter : target.sageMakerPipelineParameters().pipelineParameterList()) {
            sageMakerPipelineParameters.add(software.amazon.events.rule.SageMakerPipelineParameter.builder()
                    .name(sageMakerPipelineParameter.name())
                    .value(sageMakerPipelineParameter.value())
                    .build()
            );
          }

          targetBuilder.sageMakerPipelineParameters(software.amazon.events.rule.SageMakerPipelineParameters.builder()
                  .pipelineParameterList(sageMakerPipelineParameters)
                  .build()
          );
        }

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

  static DeleteRuleRequest translateToDeleteRuleRequest(final ResourceModel model) {
    CompositeId compositeId = new CompositeId(model);

    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    return DeleteRuleRequest.builder()
            .name(compositeId.ruleName)
            .eventBusName(compositeId.eventBusName)
            .build();
  }

  static RemoveTargetsRequest translateToRemoveTargetsRequest(final ResourceModel model, Collection<String> targetIds) {
    CompositeId compositeId = new CompositeId(model);

    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    return RemoveTargetsRequest.builder()
            .rule(compositeId.ruleName)
            .eventBusName(compositeId.eventBusName)
            .ids(targetIds)
            .build();
  }

  static RemoveTargetsRequest translateToRemoveTargetsRequest(final ResourceModel model, ListTargetsByRuleResponse listTargetsByRuleResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37

    ArrayList<String> targetIds = new ArrayList<>();
    for (Target target : listTargetsByRuleResponse.targets()) {
      targetIds.add(target.id());
    }

    return translateToRemoveTargetsRequest(model, targetIds);
  }

  // LIST

  static ListRulesRequest translateToListRulesRequest(final String nextToken) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L26-L31
    return ListRulesRequest.builder()
            .nextToken(nextToken)
            .build();
  }

  static ListTargetsByRuleRequest translateToListTargetsByRuleRequest(final ResourceModel model) {
    CompositeId compositeId = new CompositeId(model);

    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L20-L24
    return ListTargetsByRuleRequest.builder()
            .rule(compositeId.ruleName)
            .eventBusName(compositeId.eventBusName)
            .build();
  }

  static List<ResourceModel> translateFromListRulesResponse(final ListRulesResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(awsResponse.rules())
        .map(resource -> ResourceModel.builder()
                .arn(resource.arn())
            // include only primary identifier
            .build())
        .collect(Collectors.toList());
  }


  // OTHER

  static CloudWatchEventsRequest dummmyTranslator(final ResourceModel model) {
    return null;
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
}
