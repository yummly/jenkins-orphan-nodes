# jenkins-orphan-nodes

Find Jenkins EC2 build nodes that Jenkins has lost track of.

This is deployed as an AWS Lambda function and can be setup to run periodically.

It sends Slack notifications listing instance ids of leaked nodes. Other notification methods can be easily implemented.

## Configuration

Lambda functions are a little difficult to configure. This uses a two-stage process:

1. [A property file](resources/lambda.properties) specifies the S3 location of a configuration
file. This is included in the uberjar.
2. An EDN configuration file found at the location specified above contains other configuration,
   including Jenkins and notofication credentials.

## Installation

1. Check [the code](jenkins_orphan_nodes/core.clj). Your criteria for finding leaked Jenkins node may differ.
2. Set your bucket and prefix in resources/lambda.properties.
3. `lein uberjar`
4. Make your [config.edn](config.edn) and upload it to `s3://your-bucket/prefix/FUNCTION_NAME/config.edn`
5. Create a Lambda IAM role with at least the following permissions: `ec2:DescribeInstances`, `s3:GetObject` on your `config.edn` in S3, plus the [AWS Logs permissions](aws-logs-policy.json).
6. Create the function: `aws lambda create-function --region REGION --function-name FUNCTION_NAME --zip-file fileb://target/uberjar/jenkins-orphan-nodes-0.1.0-SNAPSHOT-standalone.jar --role ROLE_ARN --runtime java8 --handler jenkins_orphan_nodes.core.LambdaFn --timeout 59 --memory-size 512`
7. Test the function.

To upload a new version of the code:

1. `lein uberjar`
2. `aws lambda update-function-code --region REGION --function-name FUNCTION_NAME --zip-file fileb://target/uberjar/jenkins-orphan-nodes-0.1.0-SNAPSHOT-standalone.jar --publish`

## Development

The code can be tested from the REPL as normal.

The [dev.sh](dev.sh) script is an example of running the REPL process in a Docker container. I ran into problems with SSL certificate on my Jenkins server. A Clojure process running on my Mac refused to validate my certificate, but on Linux (Lambda, Docker) it worked fine.

## License

Copyright © 2016 Yummly

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
