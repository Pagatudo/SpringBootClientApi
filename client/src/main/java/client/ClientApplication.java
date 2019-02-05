package client;

import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAutoConfiguration
@EnableOAuth2Client
@RestController
public class ClientApplication   {

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}
 
	@Value("${oauth.resource:http://hostname/api/}")
	private String baseUrl;

	@Value("${oauth.authorize:http://hostname/auth/oauth/authorize}")
	private String authorizeUrl;

	@Value("${oauth.token:http://hostname/auth/oauth/token}")
	private String tokenUrl;
	
	 
	@Autowired
	private OAuth2RestTemplate restTemplate;
	
	
	@RequestMapping(value = "/AuthCallBack/{var1}/{var2}" , method = RequestMethod.GET)
	public String callBack(RequestEntity<String> re ,@PathVariable("var1") String str1, @PathVariable("var2") String str2,
							@RequestParam("code") String code) throws ParseException {
 
		ResponseEntity<String> response = null;
		ResponseEntity<String> resourceResponse = null;
		RestTemplate rt = new RestTemplate();
				
		String credentials = "client:client";
		String encodedCredentials = new String(Base64.encodeBase64(credentials.getBytes()));

		HttpHeaders headers  = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", "Basic " + encodedCredentials);
		
		
		HttpEntity<String> req = new HttpEntity<String>(headers);
		
		tokenUrl +="?code="+code;
		tokenUrl += "&grant_type=authorization_code";
		tokenUrl += "&redirect_uri=http://127.0.0.1:8181/client/AuthCallBack/variable1/variable2";

		//exchange token
		response = rt.exchange(tokenUrl, HttpMethod.POST, req, String.class);
		System.out.println("Access Token Response ---------" + response.getBody());
		 
		JSONParser parse = new JSONParser();
		JSONObject jsBody = (JSONObject) parse.parse(response.getBody());
		
		// send request to get statement service 
		resourceResponse = rt.exchange(baseUrl + "Members/Statement?access_token="+jsBody.get("access_token").toString()+"&accountNumber=11111&beginDate=01/09/2018&endDate=30/09/2018",
										HttpMethod.GET, req, String.class);
	 	
		return resourceResponse.getBody();
	}
	
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home() {	
		restTemplate.getAccessToken();
		return "result";
	}

	
	
 	@Bean
	public OAuth2RestTemplate restTemplate() {
 		
 		AccessTokenRequest atr = new DefaultAccessTokenRequest();
 		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource(), new DefaultOAuth2ClientContext(atr));
 		
        return restTemplate;	
	}
 	
 	 
	@Bean
	protected OAuth2ProtectedResourceDetails resource() {
		
		// autherization code grant type 
		AuthorizationCodeResourceDetails resource = new AuthorizationCodeResourceDetails();
		resource.setAccessTokenUri(tokenUrl);
		resource.setUserAuthorizationUri(authorizeUrl);
		resource.setClientId("client");
		resource.setClientSecret("client");
		resource.setScope(Arrays.asList("TRUSTED","USER_FINANCIAL"));	 
		resource.setPreEstablishedRedirectUri("http://127.0.0.1:8181/client/AuthCallBack/variable1/variable2");
		return resource ;
		
	}
	
}
