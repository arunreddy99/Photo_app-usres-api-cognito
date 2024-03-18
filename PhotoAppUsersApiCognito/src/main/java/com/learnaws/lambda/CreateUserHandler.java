package com.learnaws.lambda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
//import static com.learnaws.lambda.Utils;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

/**
 * Handler for requests to Lambda function.
 */
public class CreateUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final CognitoUserService cognitoUserService;
	private final String appClientId;
	private final String appClientSecret;

	public CreateUserHandler(CognitoUserService cognitoUserService, String appClientId, String appClientSecret) {

		this.cognitoUserService = cognitoUserService;
		this.appClientId = appClientId;
		this.appClientSecret = appClientSecret;
	}

	public CreateUserHandler() {
		this.cognitoUserService = new CognitoUserService(System.getenv("AWS_REGION"));
		this.appClientId = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_ID");
		this.appClientSecret = Utils.decryptKey("MY_COGNITO_POOL_APP_CLIENT_SECRET");
	}

	public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("X-Custom-Header", "application/json");

		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

		String requestBody = input.getBody();
		LambdaLogger logger = context.getLogger();
		logger.log("Input request body : " + requestBody);

		JsonObject userDetails = null;
		try {
			userDetails = JsonParser.parseString(requestBody).getAsJsonObject();
			JsonObject createUserResult = cognitoUserService.createUser(userDetails, appClientId, appClientSecret,
					logger);
			response.withStatusCode(200);
			response.withBody(createUserResult.toString());
		} catch (AwsServiceException e) {
			// TODO: handle exception
			logger.log(e.awsErrorDetails().errorMessage());
			ErrorResponse erroResponse = new ErrorResponse(e.awsErrorDetails().errorMessage());
			String erroResponseJsonString = new GsonBuilder().serializeNulls().create().toJson(erroResponse,
					ErrorResponse.class);
			response.withStatusCode(e.awsErrorDetails().sdkHttpResponse().statusCode());
			response.withBody(erroResponseJsonString);
		} catch (Exception e) {
			logger.log(e.getMessage());

			ErrorResponse erroResponse = new ErrorResponse(" Error :" + e.getMessage());
			String erroResponseJsonString = new Gson().toJson(erroResponse, ErrorResponse.class);

			response.withStatusCode(500);
			response.withBody(erroResponseJsonString);
		}

		return response;

	}
}
