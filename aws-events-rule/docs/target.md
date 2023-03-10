# AWS::Events::Rule Target

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#inputpath" title="InputPath">InputPath</a>" : <i>String</i>,
    "<a href="#httpparameters" title="HttpParameters">HttpParameters</a>" : <i><a href="httpparameters.md">HttpParameters</a></i>,
    "<a href="#deadletterconfig" title="DeadLetterConfig">DeadLetterConfig</a>" : <i><a href="deadletterconfig.md">DeadLetterConfig</a></i>,
    "<a href="#runcommandparameters" title="RunCommandParameters">RunCommandParameters</a>" : <i><a href="runcommandparameters.md">RunCommandParameters</a></i>,
    "<a href="#inputtransformer" title="InputTransformer">InputTransformer</a>" : <i><a href="inputtransformer.md">InputTransformer</a></i>,
    "<a href="#kinesisparameters" title="KinesisParameters">KinesisParameters</a>" : <i><a href="kinesisparameters.md">KinesisParameters</a></i>,
    "<a href="#rolearn" title="RoleArn">RoleArn</a>" : <i>String</i>,
    "<a href="#redshiftdataparameters" title="RedshiftDataParameters">RedshiftDataParameters</a>" : <i><a href="redshiftdataparameters.md">RedshiftDataParameters</a></i>,
    "<a href="#input" title="Input">Input</a>" : <i>String</i>,
    "<a href="#sqsparameters" title="SqsParameters">SqsParameters</a>" : <i><a href="sqsparameters.md">SqsParameters</a></i>,
    "<a href="#ecsparameters" title="EcsParameters">EcsParameters</a>" : <i><a href="ecsparameters.md">EcsParameters</a></i>,
    "<a href="#batchparameters" title="BatchParameters">BatchParameters</a>" : <i><a href="batchparameters.md">BatchParameters</a></i>,
    "<a href="#id" title="Id">Id</a>" : <i>String</i>,
    "<a href="#arn" title="Arn">Arn</a>" : <i>String</i>,
    "<a href="#sagemakerpipelineparameters" title="SageMakerPipelineParameters">SageMakerPipelineParameters</a>" : <i><a href="sagemakerpipelineparameters.md">SageMakerPipelineParameters</a></i>,
    "<a href="#retrypolicy" title="RetryPolicy">RetryPolicy</a>" : <i><a href="retrypolicy.md">RetryPolicy</a></i>
}
</pre>

### YAML

<pre>
<a href="#inputpath" title="InputPath">InputPath</a>: <i>String</i>
<a href="#httpparameters" title="HttpParameters">HttpParameters</a>: <i><a href="httpparameters.md">HttpParameters</a></i>
<a href="#deadletterconfig" title="DeadLetterConfig">DeadLetterConfig</a>: <i><a href="deadletterconfig.md">DeadLetterConfig</a></i>
<a href="#runcommandparameters" title="RunCommandParameters">RunCommandParameters</a>: <i><a href="runcommandparameters.md">RunCommandParameters</a></i>
<a href="#inputtransformer" title="InputTransformer">InputTransformer</a>: <i><a href="inputtransformer.md">InputTransformer</a></i>
<a href="#kinesisparameters" title="KinesisParameters">KinesisParameters</a>: <i><a href="kinesisparameters.md">KinesisParameters</a></i>
<a href="#rolearn" title="RoleArn">RoleArn</a>: <i>String</i>
<a href="#redshiftdataparameters" title="RedshiftDataParameters">RedshiftDataParameters</a>: <i><a href="redshiftdataparameters.md">RedshiftDataParameters</a></i>
<a href="#input" title="Input">Input</a>: <i>String</i>
<a href="#sqsparameters" title="SqsParameters">SqsParameters</a>: <i><a href="sqsparameters.md">SqsParameters</a></i>
<a href="#ecsparameters" title="EcsParameters">EcsParameters</a>: <i><a href="ecsparameters.md">EcsParameters</a></i>
<a href="#batchparameters" title="BatchParameters">BatchParameters</a>: <i><a href="batchparameters.md">BatchParameters</a></i>
<a href="#id" title="Id">Id</a>: <i>String</i>
<a href="#arn" title="Arn">Arn</a>: <i>String</i>
<a href="#sagemakerpipelineparameters" title="SageMakerPipelineParameters">SageMakerPipelineParameters</a>: <i><a href="sagemakerpipelineparameters.md">SageMakerPipelineParameters</a></i>
<a href="#retrypolicy" title="RetryPolicy">RetryPolicy</a>: <i><a href="retrypolicy.md">RetryPolicy</a></i>
</pre>

## Properties

#### InputPath

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### HttpParameters

_Required_: No

_Type_: <a href="httpparameters.md">HttpParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DeadLetterConfig

_Required_: No

_Type_: <a href="deadletterconfig.md">DeadLetterConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RunCommandParameters

_Required_: No

_Type_: <a href="runcommandparameters.md">RunCommandParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### InputTransformer

_Required_: No

_Type_: <a href="inputtransformer.md">InputTransformer</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### KinesisParameters

_Required_: No

_Type_: <a href="kinesisparameters.md">KinesisParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RoleArn

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RedshiftDataParameters

_Required_: No

_Type_: <a href="redshiftdataparameters.md">RedshiftDataParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Input

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SqsParameters

_Required_: No

_Type_: <a href="sqsparameters.md">SqsParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EcsParameters

_Required_: No

_Type_: <a href="ecsparameters.md">EcsParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### BatchParameters

_Required_: No

_Type_: <a href="batchparameters.md">BatchParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Id

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Arn

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SageMakerPipelineParameters

_Required_: No

_Type_: <a href="sagemakerpipelineparameters.md">SageMakerPipelineParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RetryPolicy

_Required_: No

_Type_: <a href="retrypolicy.md">RetryPolicy</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
