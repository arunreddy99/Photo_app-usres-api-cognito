package com.learnaws.lambda;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.learnaws.lambda.service.CognitoUserService;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class LoginUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
	private final CognitoUserService cognitoUserService;
	private final String appClientId;
	private final String appClientSecret;
	public  LoginUserHandler() {
		this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
		this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID") ;
		this.appClientSecret= Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET") ;
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		// TODO Auto-generated method stub
		LambdaLogger logger = context.getLogger();
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		APIGatewayProxyResponseEvent response= new APIGatewayProxyResponseEvent();
		
		try {
		JsonObject loginUserDetails= JsonParser.parseString(input.getBody()).getAsJsonObject();
		
		
		JsonObject loginUserResult = cognitoUserService.loginUser(appClientId, appClientSecret, loginUserDetails, logger);
		
		
		response.withHeaders(headers);
		response.withBody(loginUserResult.toString());
		response.withStatusCode(200);
		}
		catch (AwsServiceException e) {
			logger.log(e.awsErrorDetails().errorMessage());
			
			ErrorResponse erroResponse = new ErrorResponse(e.awsErrorDetails().errorMessage());
//			GsonBuilder can ignore nulls whereas Gson cannot as in line 62 we appended some string to avoid such cases
			String erroResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(erroResponse, ErrorResponse.class);
			
			response.withStatusCode(e.awsErrorDetails().sdkHttpResponse().statusCode());
			response.withBody(erroResponseJsonString);
		}
		catch(Exception e)
		{
			logger.log(e.getMessage());
			
			ErrorResponse erroResponse = new ErrorResponse(" Error :" +e.getMessage());
			String erroResponseJsonString = new Gson().toJson(erroResponse, ErrorResponse.class);
			
			response.withStatusCode(500);
			response.withBody(erroResponseJsonString);
		}
		return response;
	}

}
