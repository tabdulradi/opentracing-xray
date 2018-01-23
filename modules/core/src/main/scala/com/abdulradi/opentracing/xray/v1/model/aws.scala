/*
 * Copyright 2017 com.abdulradi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abdulradi.opentracing.xray.v1.model

final case class SegmentAws(
  /**
    * If your application sends segments to a different AWS account, record the ID of the account running your application.
    */
  accountId: Option[String],
  /**
    * Information about an Amazon ECS container.
    */
  ecs: Option[Ecs],
  /**
    * Information about an EC2 instance.
    */
  ec2: Option[Ec2],
  /**
    * Information about an Elastic Beanstalk environment.
    * You can find this information in a file named /var/elasticbeanstalk/xray/environment.conf on the latest Elastic Beanstalk platforms.
    */
  elasticBeanstalk: ElasticBeanstalk

  // XRay object doesn't appear in specs, but exists in examples with conflicting schema, thus ignored.
  //  xray: XRay
)

final case class Ecs(
  /**
    * The container ID of the container running your application.
    */
  container: Option[String]
)

final case class Ec2(
  /**
    * The instance ID of the EC2 instance.
    */
  instanceId: Option[String],
  /**
    * The Availability Zone in which the instance is running
    */
  availabilityZone: Option[String]
)

final case class ElasticBeanstalk(
  /**
    * The name of the environment.
    */
  environmentName: Option[String],
  /**
    * The name of the application version that is currently deployed to the instance that served the request.
    */
  versionLabel: Option[String],
  /**
    * number indicating the ID of the last successful deployment to the instance that served the request.
    */
  deploymentId: Option[Int]
)

final case class SubsegmentAws(
  /**
    * The name of the API action invoked against an AWS service or resource.
    */
  operation: Option[String],
  /**
    * If your application accesses resources in a different account, or sends segments to a different account, record the ID of the account that owns the AWS resource that your application accessed.
    */
  accountId: Option[String],
  /**
    * If the resource is in a region different from your application, record the region. For example, us-west-2.
    */
  region: Option[String],
  /**
    * Unique identifier for the request.
    */
  requestId: Option[String],
  /**
    * For operations on an Amazon SQS queue, the queue's URL.
    */
  queueUrl: Option[String],
  /**
    * For operations on a DynamoDB table, the name of the table.
    */
  tableName: Option[String]
)
