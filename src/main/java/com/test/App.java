package com.test;

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

public class App implements RequestHandler<Map<String,String>, String> {

	static AWSApplicationAutoScalingClient aaClient = (AWSApplicationAutoScalingClient) AWSApplicationAutoScalingClientBuilder.standard().build();



	@Override
	public String handleRequest(Map<String,String> event, Context context) {

		String role_arn=event.get("role_arn");
		String table=event.get("table");
		int min_write_capacity=Integer.valueOf(event.get("min_write_capacity"));
		int max_write_capacity=Integer.valueOf(event.get("max_write_capacity"));

		ServiceNamespace ns = ServiceNamespace.Dynamodb;
		ScalableDimension tableWCUs = ScalableDimension.DynamodbTableWriteCapacityUnits;
		String resourceID = "table/"+table;

		System.out.println("resourceID: "+resourceID);
		System.out.println("min_write_capacity: "+min_write_capacity);
		System.out.println("max_write_capacity: "+max_write_capacity);
		System.out.println("role_arn: "+role_arn);


		// Define the scalable target
		RegisterScalableTargetRequest rstRequest = new RegisterScalableTargetRequest()
				.withServiceNamespace(ns)
				.withResourceId(resourceID)
				.withScalableDimension(tableWCUs)
				.withMinCapacity(min_write_capacity)
				.withMaxCapacity(max_write_capacity)
				.withRoleARN(role_arn);

		try {
			aaClient.registerScalableTarget(rstRequest);
		} catch (Exception e) {
			System.err.println("Unable to register scalable target: ");
			System.err.println(e.getMessage());
		}

		// Verify that the target was created
		DescribeScalableTargetsRequest dscRequest = new DescribeScalableTargetsRequest()
				.withServiceNamespace(ns)
				.withScalableDimension(tableWCUs)
				.withResourceIds(resourceID);
		try {
			DescribeScalableTargetsResult dsaResult = aaClient.describeScalableTargets(dscRequest);
			System.out.println("DescribeScalableTargets result: ");
			System.out.println(dsaResult);
			System.out.println();
		} catch (Exception e) {
			System.err.println("Unable to describe scalable target: ");
			System.err.println(e.getMessage());
		}

		System.out.println();

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
			System.err.println("Unable to put scaling policy: ");
			System.err.println(e.getMessage());
		}

		// Verify that the scaling policy was created
		DescribeScalingPoliciesRequest dspRequest = new DescribeScalingPoliciesRequest()
				.withServiceNamespace(ns)
				.withScalableDimension(tableWCUs)
				.withResourceId(resourceID);

		try {
			DescribeScalingPoliciesResult dspResult = aaClient.describeScalingPolicies(dspRequest);
			System.out.println("DescribeScalingPolicies result: ");
			System.out.println(dspResult);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unable to describe scaling policy: ");
			System.err.println(e.getMessage());
		}

		return "done";
	}
}

