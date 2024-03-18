package com.learnaws.lambda;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
//import static org.mockito.Mockito.when;
//import static org.mockito.Mockito.times;
import static org.mockito.Mockito.*;

import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.learnaws.lambda.CreateUserHandler;
import com.learnaws.lambda.constants.Constants;
import com.learnaws.lambda.service.CognitoUserService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.SdkHttpResponse;



@ExtendWith(MockitoExtension.class)
public class CreateUserHandlerTest {
	
	@Mock
	CognitoUserService cognitoUserService;
	
	@Mock
	APIGatewayProxyRequestEvent aPIGatewayProxyRequestEvent;
	
	@Mock
	Context context;
	
	@InjectMocks
	CreateUserHandler handler;

	@Mock
	LambdaLogger lambdaLoggerMock;
	
	@BeforeEach
	public void runBeforeEachTestMethod() {
		
		System.out.println("Running before each test method");
		when(context.getLogger()).thenReturn(lambdaLoggerMock);
	}
	
	@AfterEach
	public void runAfterEachTestMethod() {
		System.out.println("Running after each test method");
		
	}
	
//	@BeforeAll
//	public void runBeforeAllTestMethod() {
//		System.out.println("Running @BeforeAll  test method");
//		
//	}
//	
//	@AfterAll
//	public void runAfterAllTestMethod() {
//		System.out.println("Running @AfterAll  test method");
//		
//	}
	
	@Test
	public void testHandleRequest_whenValidDetailsProvided_returnSuccessfullResponse() {
		

//		Arrange or Given
//		when().thenReturn();
		JsonObject userDetails =new JsonObject();
		userDetails.addProperty("firstName", "Arun");
		userDetails.addProperty("lastName", "Reddy");
		userDetails.addProperty("email", "arun1@gm.com");
		userDetails.addProperty("password", "Arunpasswsord");
		userDetails.addProperty("repeatPassword", "Arunpasswsord");
		
		String userDetailsJsonString = userDetails.toString();
		when(aPIGatewayProxyRequestEvent.getBody()).thenReturn(userDetailsJsonString );
		
//		when(context.getLogger()).thenReturn(lambdaLoggerMock);
		
		JsonObject createUserResult = new JsonObject();
		createUserResult.addProperty(Constants.IS_SUCCESSFULL, true);
		createUserResult.addProperty(Constants.STATUS_CODE, 200);
		createUserResult.addProperty(Constants.COGNITO_USER_ID, UUID.randomUUID().toString());
		createUserResult.addProperty(Constants.IS_CONFIRMED, false);
		when(cognitoUserService.createUser(any(), any(), any(), any())).thenReturn(createUserResult);
		
		
//		Act or When
		APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(aPIGatewayProxyRequestEvent, context);
		String responseBody = responseEvent.getBody();
		JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();
		
		
//		Assert orThen
		verify(lambdaLoggerMock, times(1)).log(anyString());
		assertTrue(responseBodyJson.get(Constants.IS_SUCCESSFULL).getAsBoolean());
		assertEquals(200,responseBodyJson.get(Constants.STATUS_CODE).getAsInt());
		assertNotNull(responseBodyJson.get(Constants.COGNITO_USER_ID));
		assertEquals(200, responseEvent.getStatusCode(), "Successfull HTTP response should have returned status code 200");
		assertFalse(responseBodyJson.get(Constants.IS_CONFIRMED).getAsBoolean());
		
		verify(cognitoUserService, times(1)).createUser(any(), any(), any(), any());
		
  }
	
	@Test
	public void testHandleRequest_whenEmptyRequestBodyProvided_returnsErrorMessage() {
//		Arrange
		when(aPIGatewayProxyRequestEvent.getBody()).thenReturn("");
//		when(context.getLogger()).thenReturn(lambdaLoggerMock);
		
		
//		act
		
		APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(aPIGatewayProxyRequestEvent, context);
		String responseBody = responseEvent.getBody(); 
		JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();
		
//		assert
		
		assertEquals(500, responseEvent.getStatusCode());
		assertNotNull(responseBodyJson.get("message").getAsString(), "Missing the message property in Json response");
		assertFalse( "Error message should not be empty",(responseBodyJson.get("message").getAsString()).isEmpty());
	}
	
	@Test
	public void testHandleRequest_whenAWSServiceExceptionTakesPlace_returnsErrorMessage() {
//		arrange
		when(aPIGatewayProxyRequestEvent.getBody()).thenReturn("{}");
		AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder()
				.errorCode("")
				.sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build())
				.errorMessage("AwsServiceException took place").build();
		when(cognitoUserService.createUser(any(), any(), any(), any())).thenThrow(
				AwsServiceException.builder()
				.statusCode(500)
				.awsErrorDetails(awsErrorDetails).build());
		
//		act
		APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(aPIGatewayProxyRequestEvent, context);
		String responseBody = responseEvent.getBody(); 
		JsonObject responseBodyJson = JsonParser.parseString(responseBody).getAsJsonObject();
		
//		assert
		assertEquals(awsErrorDetails.sdkHttpResponse().statusCode(), responseEvent.getStatusCode());
		assertNotNull(responseBodyJson.get("message"));
		assertEquals(awsErrorDetails.errorMessage(), responseBodyJson.get("message").getAsString());
		
		
	}
	
}
