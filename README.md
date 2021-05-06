# dynamodb-capacity-change
Automatic Dynamodb Capacity Change Lambda

Initial work done by Choolake Suwandarathna. It might be based on the help from AWS support services.
I have done some modifications and testing.

Create the AWS Lambda with target jar file. You have to invoke it with following payload:
{
  "role_arn": "arn:aws:iam::<account id>:role/<role name>",
  "table": "<dynamodb table name>",
  "min_write_capacity": "<value>",
  "max_write_capacity": "<value>"
}
