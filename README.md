# DynamoDB Provisioned Capacity Adjuster
This lambda function will adjust your AWS dynamodb write capacity on the fly. If you need to adjust the provisioned capacity to a specific value during a time window, this is your ticket home.

Contributers
@C4Jay 
@kokilahettiarachchi
@dialoglk

Create the AWS Lambda with target jar file. You have to invoke it with following payload:
```json
{
  "role_arn": "arn:aws:iam::<account id>:role/<role name>",
  "table": "<dynamodb table name>",
  "min_write_capacity": "<value>",
  "max_write_capacity": "<value>"
}
```
