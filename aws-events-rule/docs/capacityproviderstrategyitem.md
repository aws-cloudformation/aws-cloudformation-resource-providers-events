# AWS::Events::Rule CapacityProviderStrategyItem

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#base" title="Base">Base</a>" : <i>Integer</i>,
    "<a href="#weight" title="Weight">Weight</a>" : <i>Integer</i>,
    "<a href="#capacityprovider" title="CapacityProvider">CapacityProvider</a>" : <i>String</i>
}
</pre>

### YAML

<pre>
<a href="#base" title="Base">Base</a>: <i>Integer</i>
<a href="#weight" title="Weight">Weight</a>: <i>Integer</i>
<a href="#capacityprovider" title="CapacityProvider">CapacityProvider</a>: <i>String</i>
</pre>

## Properties

#### Base

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Weight

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### CapacityProvider

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
