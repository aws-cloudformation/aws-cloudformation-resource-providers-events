STACK_NAME=CFNRegistryAWSEventsEventBusStack
TMP_DIR=/tmp/uluru_test_accounts_config

rm -rf $TMP_DIR
mkdir -p $TMP_DIR

function cache_regional_json() {
  ACCOUNT_ID=$1
  REGION=$2
  PARTITION=$3

  echo Caching contract tests account config for $REGION

  ada credentials update --account=$ACCOUNT_ID --provider=isengard --role=Administrator --partition $PARTITION --once

  aws --region $REGION cloudformation describe-stack-resources --stack-name $STACK_NAME | jq -r --arg REGION $REGION --arg ACCOUNT_ID $ACCOUNT_ID --arg PARTITION $PARTITION '[
      {regionName: $REGION},
      (.StackResources[] | select(.LogicalResourceId=="CfnContractTestsRole") | {iamRoleArn: ("arn:" + $PARTITION + ":iam::" + $ACCOUNT_ID + ":role/" + .PhysicalResourceId)}),
      (.StackResources[] | select(.LogicalResourceId=="CfnContractTestsBucket") | {s3BucketName: .PhysicalResourceId}),
      (.StackResources[] | select(.LogicalResourceId=="CfnContractTestsKey") | {kmsKeyArn: ("arn:" + $PARTITION + ":kms:" + $REGION + ":" + $ACCOUNT_ID + ":key/" + .PhysicalResourceId)})
    ] | add' | tee $TMP_DIR/regional_data_$REGION.json
}
cache_regional_json 307042904846 af-south-1 aws
cache_regional_json 417527047072 ap-east-1 aws
cache_regional_json 887577080403 ap-northeast-1 aws
cache_regional_json 334039033025 ap-northeast-2 aws
cache_regional_json 730441393695 ap-northeast-3 aws
cache_regional_json 076109168760 ap-south-1 aws
cache_regional_json 627523199635 ap-south-2 aws
cache_regional_json 616929341984 ap-southeast-1 aws
cache_regional_json 509651502529 ap-southeast-2 aws
cache_regional_json 057893041956 ap-southeast-3 aws
cache_regional_json 326487361931 ap-southeast-4 aws
cache_regional_json 985102208424 ca-central-1 aws
cache_regional_json 722122155426 eu-central-1 aws
cache_regional_json 578555297688 eu-central-2 aws
cache_regional_json 005059088433 eu-north-1 aws
cache_regional_json 375192440144 eu-south-1 aws
cache_regional_json 922588172390 eu-south-2 aws
cache_regional_json 404929930276 eu-west-1 aws
cache_regional_json 733369302651 eu-west-2 aws
cache_regional_json 164282127974 eu-west-3 aws
cache_regional_json 067897675478 me-central-1 aws
cache_regional_json 251017616061 me-south-1 aws
cache_regional_json 271154368589 sa-east-1 aws
cache_regional_json 098059526382 us-east-1 aws
cache_regional_json 652421452304 us-east-2 aws
cache_regional_json 980818222174 us-west-1 aws
cache_regional_json 024129877953 us-west-2 aws
cache_regional_json 801940705109 il-central-1 aws
cache_regional_json 434104914144 cn-north-1 aws-cn
cache_regional_json 442446944583 cn-northwest-1 aws-cn



jq -n '.testAccounts |= [inputs]' $TMP_DIR/regional_data_*.json > $TMP_DIR/testAccountsConfig.json

(echo "# This is a generated file - do not edit! See README on how to generate testAccountsConfig.yml"; yq -Poy $TMP_DIR/testAccountsConfig.json) > $TMP_DIR/testAccountsConfig.yaml

cp $TMP_DIR/testAccountsConfig.yaml contract-tests-artifacts

rm -rf $TMP_DIR 

echo "All done! testAccountsConfig.yml is copied to contract-tests-artifacts/ directories in all projects."

