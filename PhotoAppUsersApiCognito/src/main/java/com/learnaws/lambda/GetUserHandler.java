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
import com.learnaws.lambda.service.CognitoUserService;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private final CognitoUserService cognitoUserService;
	
	public GetUserHandler() {
		this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		
		LambdaLogger logger = context.getLogger();
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		APIGatewayProxyResponseEvent response= new APIGatewayProxyResponseEvent();
		String accessToken = input.getHeaders().get("AccessToken");
		try {
			
			JsonObject getUserResult = cognitoUserService.getUser(accessToken);
			response.withBody(getUserResult.toString());
			response.withStatusCode(200);
		} catch (AwsServiceException e) {
			logger.log(e.awsErrorDetails().errorMessage());
			ErrorResponse erroResponse = new ErrorResponse(e.awsErrorDetails().errorMessage());
			String erroResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(erroResponse, ErrorResponse.class);
			response.withStatusCode(e.awsErrorDetails().sdkHttpResponse().statusCode());
			response.withBody(erroResponseJsonString);
		}
		catch(Exception e)
		{
			logger.log(e.getMessage());
			ErrorResponse erroResponse = new ErrorResponse(e.getMessage());
			String erroResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(erroResponse, ErrorResponse.class);
			response.withStatusCode(500);
			response.withBody(erroResponseJsonString);
		}
		

		return response;
	}

}
