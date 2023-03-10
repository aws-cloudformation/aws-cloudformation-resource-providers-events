# AWS::Events::Rule HttpParameters

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#pathparametervalues" title="PathParameterValues">PathParameterValues</a>" : <i>[ String, ... ]</i>,
    "<a href="#headerparameters" title="HeaderParameters">HeaderParameters</a>" : <i><a href="httpparameters-headerparameters.md">HeaderParameters</a></i>,
    "<a href="#querystringparameters" title="QueryStringParameters">QueryStringParameters</a>" : <i><a href="httpparameters-querystringparameters.md">QueryStringParameters</a></i>
}
</pre>

### YAML

<pre>
<a href="#pathparametervalues" title="PathParameterValues">PathParameterValues</a>: <i>
      - String</i>
<a href="#headerparameters" title="HeaderParameters">HeaderParameters</a>: <i><a href="httpparameters-headerparameters.md">HeaderParameters</a></i>
<a href="#querystringparameters" title="QueryStringParameters">QueryStringParameters</a>: <i><a href="httpparameters-querystringparameters.md">QueryStringParameters</a></i>
</pre>

## Properties

#### PathParameterValues

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### HeaderParameters

_Required_: No

_Type_: <a href="httpparameters-headerparameters.md">HeaderParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### QueryStringParameters

_Required_: No

_Type_: <a href="httpparameters-querystringparameters.md">QueryStringParameters</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
