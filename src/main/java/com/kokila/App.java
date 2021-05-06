package com.kokila;

//This file is originally created by Choolake Suwandarathna.
//It might be based on the help from AWS support services.
//I have done some small modifications and testing.


import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClient;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClientBuilder;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsResult;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalingPoliciesRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalingPoliciesResult;
import com.amazonaws.services.applicationautoscaling.model.MetricType;
import com.amazonaws.services.applicationautoscaling.model.PolicyType;
import com.amazonaws.services.applicationautoscaling.model.PredefinedMetricSpecification;
import com.amazonaws.services.applicationautoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.applicationautoscaling.model.RegisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.ScalableDimension;
import com.amazonaws.services.applicationautoscaling.model.ServiceNamespace;
import com.amazonaws.services.applicationautoscaling.model.TargetTrackingScalingPolicyConfiguration;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App implements RequestHandler<Map<String,String>, String> {
	private static Logger LOGGER = LoggerFactory.getLogger(App.class);
	private static AWSApplicationAutoScalingClient aaClient = (AWSApplicationAutoScalingClient) AWSApplicationAutoScalingClientBuilder.standard().build();


	@Override
	public String handleRequest(Map<String,String> event, Context context) {

		String roleArn = event.get("role_arn");
		String table = event.get("table");
		int minWriteCapacity = Integer.parseInt(event.get("min_write_capacity"));
		int maxWriteCapacity = Integer.parseInt(event.get("max_write_capacity"));

		ServiceNamespace ns = ServiceNamespace.Dynamodb;
		ScalableDimension tableWCUs = ScalableDimension.DynamodbTableWriteCapacityUnits;
		String resourceID = "table/"+table;


		LOGGER.info("run date: {}", resourceID);
		LOGGER.info("min_write_capacity: {}", minWriteCapacity);
		LOGGER.info("max_write_capacity: {}", maxWriteCapacity);
		LOGGER.info("role_arn: {}", roleArn);


		// Define the scalable target
		RegisterScalableTargetRequest rstRequest = new RegisterScalableTargetRequest()
				.withServiceNamespace(ns)
				.withResourceId(resourceID)
				.withScalableDimension(tableWCUs)
				.withMinCapacity(minWriteCapacity)
				.withMaxCapacity(maxWriteCapacity)
				.withRoleARN(roleArn);

		try {
			aaClient.registerScalableTarget(rstRequest);
		} catch (Exception e) {
			LOGGER.error("Unable to register scalable target: ", e);
		}

		// Verify that the target was created
		DescribeScalableTargetsRequest dscRequest = new DescribeScalableTargetsRequest()
				.withServiceNamespace(ns)
				.withScalableDimension(tableWCUs)
				.withResourceIds(resourceID);
		try {
			DescribeScalableTargetsResult dsaResult = aaClient.describeScalableTargets(dscRequest);

			LOGGER.error("Unable to register scalable target: ");
			LOGGER.error(String.valueOf(dsaResult));
		} catch (Exception e) {
			LOGGER.error("Unable to describe scalable target: ");
			LOGGER.error(e.getMessage());
		}


		// Configure a scaling policy
		TargetTrackingScalingPolicyConfiguration targetTrackingScalingPolicyConfiguration =
				new TargetTrackingScalingPolicyConfiguration()
						.withPredefinedMetricSpecification(
								new PredefinedMetricSpecification()
										.withPredefinedMetricType(MetricType.DynamoDBWriteCapacityUtilization))
						.withTargetValue(90.0)
						.withScaleInCooldown(10)
						.withScaleOutCooldown(10);

		// Create the scaling policy, based on your configuration
		PutScalingPolicyRequest pspRequest = new PutScalingPolicyRequest()
				.withServiceNamespace(ns)
				.withScalableDimension(tableWCUs)
				.withResourceId(resourceID)
				.withPolicyName("MyScalingPolicy")
				.withPolicyType(PolicyType.TargetTrackingScaling)
				.withTargetTrackingScalingPolicyConfiguration(targetTrackingScalingPolicyConfiguration);

		try {
			aaClient.putScalingPolicy(pspRequest);
		} catch (Exception e) {
			LOGGER.error("Unable to put scaling policy: ");
			LOGGER.error(e.getMessage());
		}

		// Verify that the scaling policy was created
		DescribeScalingPoliciesRequest dspRequest = new DescribeScalingPoliciesRequest()
				.withServiceNamespace(ns)
				.withScalableDimension(tableWCUs)
				.withResourceId(resourceID);

		try {
			DescribeScalingPoliciesResult dspResult = aaClient.describeScalingPolicies(dspRequest);
			LOGGER.error("DescribeScalingPolicies result: ");
			LOGGER.debug(String.valueOf(dspResult));
		} catch (Exception e) {
			LOGGER.error("Unable to describe scaling policy: ", e);
		}

		return "done";
	}
}

