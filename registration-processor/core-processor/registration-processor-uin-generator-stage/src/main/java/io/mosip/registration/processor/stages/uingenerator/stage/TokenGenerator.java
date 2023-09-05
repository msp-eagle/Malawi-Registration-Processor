package io.mosip.registration.processor.stages.uingenerator.stage;

import com.google.gson.Gson;
import io.mosip.registration.processor.stages.uingenerator.dto.ClientIdSecretKeyRequestDto;
import io.mosip.registration.processor.stages.uingenerator.dto.NewTokenRequestDto;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class TokenGenerator {

    @Value("${token.request.appid}")
    public String appid;

    @Value("${token.request.clientId}")
    public String clientId;

    @Value("${token.request.secretKey}")
    public String secretKey;

    @Value("${KEYBASEDTOKENAPI}")
    public String authUrl;

    @Autowired
    Environment environment;

//	private static final String AUTHORIZATION = "Authorization=";


    /**
     * This method gets the token for the user details present in config server.
     *
     * @return
     * @throws IOException
     */
    public String getToken() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

            return generateToken(setRequestDto());


    }

    public String generateToken(ClientIdSecretKeyRequestDto dto) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        String token = null;

            NewTokenRequestDto tokenRequest = new NewTokenRequestDto();
            tokenRequest.setId("String");

            tokenRequest.setRequesttime(getUTCCurrentDateTime().toString());
            tokenRequest.setRequest(dto);
            tokenRequest.setVersion("String");

            Gson gson = new Gson();
            HttpClient httpClient ;

            httpClient = HttpClients
                    .custom()
                    .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
            HttpPost post = new HttpPost(authUrl);
            try {
                System.out.println("request body : "+gson.toJson(tokenRequest));
                StringEntity postingString = new StringEntity(gson.toJson(tokenRequest));
                post.setEntity(postingString);
                post.setHeader("Content-type", "application/json");
                HttpResponse response = httpClient.execute(post);

                org.apache.http.HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, "UTF-8");
                System.out.println("response"+responseBody);
                Header[] cookie = response.getHeaders("Set-Cookie");

//                    throw new TokenGenerationFailedException();
                token = response.getHeaders("Set-Cookie")[0].getValue();
//                logger.info(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
//                        LoggerFileConstant.APPLICATIONID.toString(), "Cookie => " + cookie[0]);
                return token.substring(0, token.indexOf(';'));
            } catch (IOException e) {
//                logger.error(LoggerFileConstant.SESSIONID.toString(), LoggerFileConstant.APPLICATIONID.toString(),
//                        LoggerFileConstant.APPLICATIONID.toString(), e.getMessage() + ExceptionUtils.getStackTrace(e));
                e.printStackTrace();
                throw e;
            }

    }

    public ClientIdSecretKeyRequestDto setRequestDto() {
        ClientIdSecretKeyRequestDto request = new ClientIdSecretKeyRequestDto();
        request.setAppId(appid);
        request.setClientId(clientId);
        request.setSecretKey(secretKey);
        return request;
    }


    public LocalDateTime getUTCCurrentDateTime() {
        return ZonedDateTime.now(ZoneOffset.UTC).toLocalDateTime();
    }

}
