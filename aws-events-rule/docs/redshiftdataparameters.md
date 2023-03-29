# AWS::Events::Rule RedshiftDataParameters

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#statementname" title="StatementName">StatementName</a>" : <i>String</i>,
    "<a href="#database" title="Database">Database</a>" : <i>String</i>,
    "<a href="#secretmanagerarn" title="SecretManagerArn">SecretManagerArn</a>" : <i>String</i>,
    "<a href="#dbuser" title="DbUser">DbUser</a>" : <i>String</i>,
    "<a href="#sql" title="Sql">Sql</a>" : <i>String</i>,
    "<a href="#withevent" title="WithEvent">WithEvent</a>" : <i>Boolean</i>
}
</pre>

### YAML

<pre>
<a href="#statementname" title="StatementName">StatementName</a>: <i>String</i>
<a href="#database" title="Database">Database</a>: <i>String</i>
<a href="#secretmanagerarn" title="SecretManagerArn">SecretManagerArn</a>: <i>String</i>
<a href="#dbuser" title="DbUser">DbUser</a>: <i>String</i>
<a href="#sql" title="Sql">Sql</a>: <i>String</i>
<a href="#withevent" title="WithEvent">WithEvent</a>: <i>Boolean</i>
</pre>

## Properties

#### StatementName

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Database

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### SecretManagerArn

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### DbUser

_Required_: No

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Sql

_Required_: Yes

_Type_: String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### WithEvent

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

