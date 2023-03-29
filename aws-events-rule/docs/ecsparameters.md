# AWS::Events::Rule EcsParameters

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#platformversion" title="PlatformVersion">PlatformVersion</a>" : <i>String</i>,
    "<a href="#group" title="Group">Group</a>" : <i>String</i>,
    "<a href="#enableecsmanagedtags" title="EnableECSManagedTags">EnableECSManagedTags</a>" : <i>Boolean</i>,
    "<a href="#enableexecutecommand" title="EnableExecuteCommand">EnableExecuteCommand</a>" : <i>Boolean</i>,
    "<a href="#placementconstraints" title="PlacementConstraints">PlacementConstraints</a>" : <i>[ <a href="placementconstraint.md">PlacementConstraint</a>, ... ]</i>,
    "<a href="#propagatetags" title="PropagateTags">PropagateTags</a>" : <i>String</i>,
    "<a href="#taskcount" title="TaskCount">TaskCount</a>" : <i>Integer</i>,
    "<a href="#placementstrategies" title="PlacementStrategies">PlacementStrategies</a>" : <i>[ <a href="placementstrategy.md">PlacementStrategy</a>, ... ]</i>,
    "<a href="#capacityproviderstrategy" title="CapacityProviderStrategy">CapacityProviderStrategy</a>" : <i>[ <a href="capacityproviderstrategyitem.md">CapacityProviderStrategyItem</a>, ... ]</i>,
    "<a href="#launchtype" title="LaunchType">LaunchType</a>" : <i>String</i>,
    "<a href="#referenceid" title="ReferenceId">ReferenceId</a>" : <i>String</i>,
    "<a href="#taglist" title="TagList">TagList</a>" : <i>[ <a href="tag.md">Tag</a>, ... ]</i>,
    "<a href="#networkconfiguration" title="NetworkConfiguration">NetworkConfiguration</a>" : <i><a href="networkconfiguration.md">NetworkConfiguration</a></i>,
    "<a href="#taskdefinitionarn" title="TaskDefinitionArn">TaskDefinitionArn</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#platformversion" title="PlatformVersion">PlatformVersion</a>: <i>String</i>
<a href="#group" title="Group">Group</a>: <i>String</i>
<a href="#enableecsmanagedtags" title="EnableECSManagedTags">EnableECSManagedTags</a>: <i>Boolean</i>
<a href="#enableexecutecommand" title="EnableExecuteCommand">EnableExecuteCommand</a>: <i>Boolean</i>
<a href="#placementconstraints" title="PlacementConstraints">PlacementConstraints</a>: <i>
      - <a href="placementconstraint.md">PlacementConstraint</a></i>
<a href="#propagatetags" title="PropagateTags">PropagateTags</a>: <i>String</i>
<a href="#taskcount" title="TaskCount">TaskCount</a>: <i>Integer</i>
<a href="#placementstrategies" title="PlacementStrategies">PlacementStrategies</a>: <i>
      - <a href="placementstrategy.md">PlacementStrategy</a></i>
<a href="#capacityproviderstrategy" title="CapacityProviderStrategy">CapacityProviderStrategy</a>: <i>
      - <a href="capacityproviderstrategyitem.md">CapacityProviderStrategyItem</a></i>
<a href="#launchtype" title="LaunchType">LaunchType</a>: <i>String</i>
<a href="#referenceid" title="ReferenceId">ReferenceId</a>: <i>String</i>
<a href="#taglist" title="TagList">TagList</a>: <i>
      - <a href="tag.md">Tag</a></i>
<a href="#networkconfiguration" title="NetworkConfiguration">NetworkConfiguration</a>: <i><a href="networkconfiguration.md">NetworkConfiguration</a></i>
<a href="#taskdefinitionarn" title="TaskDefinitionArn">TaskDefinitionArn</a>: <i>String</i>
</pre>

## Properties

#### PlatformVersion

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Group

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EnableECSManagedTags

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### EnableExecuteCommand

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PlacementConstraints

_Required_: No

_Type_: List of <a href="placementconstraint.md">PlacementConstraint</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PropagateTags

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TaskCount

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### PlacementStrategies

_Required_: No

_Type_: List of <a href="placementstrategy.md">PlacementStrategy</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CapacityProviderStrategy

_Required_: No

_Type_: List of <a href="capacityproviderstrategyitem.md">CapacityProviderStrategyItem</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### LaunchType

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### ReferenceId

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TagList

_Required_: No

_Type_: List of <a href="tag.md">Tag</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### NetworkConfiguration

_Required_: No

_Type_: <a href="networkconfiguration.md">NetworkConfiguration</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TaskDefinitionArn

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

