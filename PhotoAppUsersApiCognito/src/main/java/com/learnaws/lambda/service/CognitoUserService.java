package com.learnaws.lambda.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonObject;
import com.learnaws.lambda.constants.Constants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

public class CognitoUserService {
	private final CognitoIdentityProviderClient cognitoIdentityProviderClient;

	public CognitoUserService(String region) {
		this.cognitoIdentityProviderClient = CognitoIdentityProviderClient.builder().region(Region.of(region)).build();
	}

	public CognitoUserService(CognitoIdentityProviderClient cognitoIdentityProviderClient) {
		this.cognitoIdentityProviderClient = cognitoIdentityProviderClient;

	}

//private static final Logger logger = LoggerFactory.getLogger(CognitoUserService.class.getName());
	public JsonObject createUser(JsonObject user, String appClientId, String appClientSecret, LambdaLogger logger) {
		logger.log("Enter create User method");

		String email = user.get("email").getAsString();
		String password = user.get("password").getAsString();
		String userId = UUID.randomUUID().toString();
		String firstName = user.get("firstName").getAsString();
		String lastName = user.get("lastName").getAsString();

		AttributeType emailAttribute = AttributeType.builder().name("email").value(email).build();
		AttributeType nameAttribute = AttributeType.builder().name("name").value(firstName + " " + lastName).build();
		AttributeType userIdAttribute = AttributeType.builder().name("custom:userId").value(userId).build();

		List<AttributeType> attributes = new ArrayList();
		attributes.add(emailAttribute);
		attributes.add(nameAttribute);
		attributes.add(userIdAttribute);
		logger.log("Attributes list : " + attributes);
		String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);
		logger.log("generated secret hash: " + generatedSecretHash);
		SignUpRequest signUpRequest = SignUpRequest.builder().username(email).password(password)
				.userAttributes(attributes).clientId(appClientId).secretHash(generatedSecretHash).build();
		logger.log("Generated sign up request: " + signUpRequest);
		SignUpResponse signUpResponse;

		signUpResponse = cognitoIdentityProviderClient.signUp(signUpRequest);

		logger.log("Signup Response :" + signUpResponse);
		JsonObject createUserResult = new JsonObject();
		createUserResult.addProperty(Constants.IS_SUCCESSFULL, signUpResponse.sdkHttpResponse().isSuccessful());
		createUserResult.addProperty(Constants.STATUS_CODE, signUpResponse.sdkHttpResponse().statusCode());
		createUserResult.addProperty(Constants.COGNITO_USER_ID, signUpResponse.userSub());
		createUserResult.addProperty(Constants.IS_CONFIRMED, signUpResponse.userConfirmed());

		return createUserResult;
	}

	public JsonObject confirmUserSignup(String userPoolAppClientId, String appClientSecret, String email,
			String confirmationCode, LambdaLogger logger) {

		String generatedSecretHash = calculateSecretHash(userPoolAppClientId, appClientSecret, email);

		ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder().secretHash(generatedSecretHash)
				.username(email).confirmationCode(confirmationCode).clientId(userPoolAppClientId).build();

		ConfirmSignUpResponse confirmSignUpResponse = cognitoIdentityProviderClient.confirmSignUp(confirmSignUpRequest);

		JsonObject confirmUserResponse = new JsonObject();
		confirmUserResponse.addProperty(Constants.IS_SUCCESSFULL, confirmSignUpResponse.sdkHttpResponse().isSuccessful());
		confirmUserResponse.addProperty(Constants.STATUS_CODE, confirmSignUpResponse.sdkHttpResponse().statusCode());
		return confirmUserResponse;

	}

	public JsonObject loginUser(String appClientId, String appClientSecret, JsonObject loginUserDetails,
			LambdaLogger logger) {

		String email = loginUserDetails.get("email").getAsString();
		String password = loginUserDetails.get("password").getAsString();
		String generatedSecretHash = calculateSecretHash(appClientId, appClientSecret, email);
		Map<String, String> authParameters = new HashMap() {
			{
				put("USERNAME", email);
				put("PASSWORD", password);
				put("SECRET_HASH", generatedSecretHash);
			}
		};

		logger.log("authParameters :" + authParameters);
		InitiateAuthRequest initiateAuthRequest = InitiateAuthRequest.builder().authParameters(authParameters)
				.clientId(appClientId).authFlow(AuthFlowType.USER_PASSWORD_AUTH).build();

		InitiateAuthResponse initateAuthResponse = cognitoIdentityProviderClient.initiateAuth(initiateAuthRequest);
		AuthenticationResultType authenticationResultType = initateAuthResponse.authenticationResult();

		JsonObject loginUserResponse = new JsonObject();
		loginUserResponse.addProperty(Constants.IS_SUCCESSFULL, initateAuthResponse.sdkHttpResponse().isSuccessful());
		loginUserResponse.addProperty(Constants.STATUS_CODE, initateAuthResponse.sdkHttpResponse().statusCode());
		loginUserResponse.addProperty(Constants.ID_TOKEN, authenticationResultType.idToken());
		loginUserResponse.addProperty("access Token", authenticationResultType.accessToken());
		loginUserResponse.addProperty("refresh Token", authenticationResultType.refreshToken());

		return loginUserResponse;
	}

	public JsonObject addUserToGroup(String email, String groupName, String userPoolId) {
		AdminAddUserToGroupRequest adminAddUserToGroupRequest = AdminAddUserToGroupRequest.builder()
				.username(email)
				.groupName(groupName)
				.userPoolId(userPoolId)
				.build();
		
		AdminAddUserToGroupResponse adminAddUserToGroupResponse =cognitoIdentityProviderClient.adminAddUserToGroup(adminAddUserToGroupRequest); 
		
		JsonObject addUserToGroupResponse = new JsonObject();
		addUserToGroupResponse.addProperty(Constants.IS_SUCCESSFULL, adminAddUserToGroupResponse.sdkHttpResponse().isSuccessful());
		addUserToGroupResponse.addProperty(Constants.STATUS_CODE, adminAddUserToGroupResponse.sdkHttpResponse().statusCode());
		
		return addUserToGroupResponse;

	}
	
	public JsonObject getUser(String accessToken) {
		GetUserRequest getUserRequest = GetUserRequest.builder().accessToken(accessToken).build();
		GetUserResponse getUserResponse = cognitoIdentityProviderClient.getUser(getUserRequest);
		
		JsonObject getUserResult = new JsonObject();
		getUserResult.addProperty(Constants.IS_SUCCESSFULL, getUserResponse.sdkHttpResponse().isSuccessful());
		getUserResult.addProperty(Constants.STATUS_CODE, getUserResponse.sdkHttpResponse().statusCode());
		
		List<AttributeType> userAttributes = getUserResponse.userAttributes();
		JsonObject userDetails = new JsonObject();
		userAttributes.stream().forEach((attribute) ->{
			userDetails.addProperty(attribute.name(), attribute.value());
		});
		
		getUserResult.add("user", userDetails);
		
		return getUserResult;
	}
	
	public String calculateSecretHash(String userPoolClientId, String userPoolClientSecret, String userName) {
		final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

		SecretKeySpec signingKey = new SecretKeySpec(userPoolClientSecret.getBytes(StandardCharsets.UTF_8),
				HMAC_SHA256_ALGORITHM);
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(signingKey);
			mac.update(userName.getBytes(StandardCharsets.UTF_8));
			byte[] rawHmac = mac.doFinal(userPoolClientId.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(rawHmac);
		} catch (Exception e) {
			throw new RuntimeException("Error while calculating ");
		}
	}

}
