package com.learnaws.lambda;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.learnaws.lambda.service.CognitoUserService;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.regions.Region;

public class AddUserToGroupHandler
		implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private final CognitoUserService cognitoUserService;
	private final String userPoolId;

	public  AddUserToGroupHandler() {
		// TODO Auto-generated constructor stub
		this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
		this.userPoolId = Utils.decryptKey("MY_COGNITO_POOL_ID");
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		// TODO Auto-generated method stub
		Map<String, String> headers = new HashMap();
		headers.put("Content-Type", "application/json");
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		response.withHeaders(headers);
		LambdaLogger logger = context.getLogger();

		try {
			JsonObject requestBody = JsonParser.parseString(input.getBody()).getAsJsonObject();

			String groupName = requestBody.get("groupName").getAsString();
			String userName = input.getPathParameters().get("userName");

			JsonObject addUserToGroupResponse = cognitoUserService.addUserToGroup(userName, groupName, userPoolId);
			response.withStatusCode(200);
			response.withBody(addUserToGroupResponse.toString());

		} catch (AwsServiceException e) {
			logger.log(e.awsErrorDetails().errorMessage());
			ErrorResponse erroResponse = new ErrorResponse(e.awsErrorDetails().errorMessage());
			String erroResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(erroResponse,
					ErrorResponse.class);
			response.withStatusCode(e.awsErrorDetails().sdkHttpResponse().statusCode());
			response.withBody(erroResponseJsonString);
		} catch (Exception e) {
			logger.log(e.getMessage());
			ErrorResponse erroResponse = new ErrorResponse(e.getMessage());
			String erroResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(erroResponse,
					ErrorResponse.class);
			response.withStatusCode(500);
			response.withBody(erroResponseJsonString);
		}
		return response;
	}

}
