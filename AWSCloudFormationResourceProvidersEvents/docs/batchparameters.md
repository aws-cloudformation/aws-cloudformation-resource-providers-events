# AWS::Events::Rule BatchParameters

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#jobname" title="JobName">JobName</a>" : <i>String</i>,
    "<a href="#retrystrategy" title="RetryStrategy">RetryStrategy</a>" : <i><a href="batchretrystrategy.md">BatchRetryStrategy</a></i>,
    "<a href="#arrayproperties" title="ArrayProperties">ArrayProperties</a>" : <i><a href="batcharrayproperties.md">BatchArrayProperties</a></i>,
    "<a href="#jobdefinition" title="JobDefinition">JobDefinition</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#jobname" title="JobName">JobName</a>: <i>String</i>
<a href="#retrystrategy" title="RetryStrategy">RetryStrategy</a>: <i><a href="batchretrystrategy.md">BatchRetryStrategy</a></i>
<a href="#arrayproperties" title="ArrayProperties">ArrayProperties</a>: <i><a href="batcharrayproperties.md">BatchArrayProperties</a></i>
<a href="#jobdefinition" title="JobDefinition">JobDefinition</a>: <i>String</i>
</pre>

## Properties

#### JobName

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### RetryStrategy

_Required_: No

_Type_: <a href="batchretrystrategy.md">BatchRetryStrategy</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ArrayProperties

_Required_: No

_Type_: <a href="batcharrayproperties.md">BatchArrayProperties</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### JobDefinition

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

