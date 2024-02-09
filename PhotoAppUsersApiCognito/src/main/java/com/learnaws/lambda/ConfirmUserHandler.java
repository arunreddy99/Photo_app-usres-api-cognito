package com.learnaws.lambda;

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

public class ConfirmUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final CognitoUserService cognitoUserService;
	private final String appClientSecret;
	private final String appClientId;

	public ConfirmUserHandler() {
		this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
		this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID") ;
		this.appClientSecret= Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET") ;
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		// TODO Auto-generated method stub
		LambdaLogger logger = context.getLogger();
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

		try {
			String requestBodyJsonString = input.getBody();
			JsonObject requestBody = JsonParser.parseString(requestBodyJsonString).getAsJsonObject();
			String email = requestBody.get("email").getAsString();
			String confirmationCode = requestBody.get("code").getAsString();
			logger.log("/n app client id:"+ appClientId+ ".");
			logger.log("/n app client secret:"+ appClientSecret+ ".");
			JsonObject confirmUserResult = cognitoUserService.confirmUserSignup(appClientId, appClientSecret, email,
					confirmationCode, logger);
			response.withStatusCode(200);
			response.withBody(confirmUserResult.toString());
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
