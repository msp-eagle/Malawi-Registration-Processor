package io.mosip.registration.processor.stages.uingenerator.stage;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;

@Component
public class ConnectApi {
    private HttpEntity<Object> setRequestHeader(Object requestType, MediaType mediaType, String token)
            throws IOException {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        headers.add("Cookie", token);
        headers.add("Authorization", token);
        if (mediaType != null) {
            headers.add("Content-Type", mediaType.toString());

        }
        if (requestType != null) {
            try {
                HttpEntity<Object> httpEntity = (HttpEntity<Object>) requestType;
                HttpHeaders httpHeader = httpEntity.getHeaders();
                Iterator<String> iterator = httpHeader.keySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (!(headers.containsKey("Content-Type") && key == "Content-Type"))
                        headers.add(key, httpHeader.get(key).get(0));
                }
                return new HttpEntity<Object>(httpEntity.getBody(), headers);
            } catch (ClassCastException e) {
                return new HttpEntity<Object>(requestType, headers);
            }
        } else
            return new HttpEntity<Object>(headers);
    }


    public static String gethttp(String http_url, String TOKEN) throws IOException {
        OutputStream ouputStream = null;
        String searchFieldResponse = null;

        try {
            turnOffSslChecking();
            URLConnection url = new URL(http_url).openConnection();
            url.setRequestProperty("Content-Type", "application/json");
            url.setRequestProperty("Accept", "application/json");
            url.setDoOutput(true);
            url.setRequestProperty("Cookie", TOKEN);
//            url.setRequestProperty("Cookie", tokenGenerator.getToken());
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.getInputStream()));
            String tmpStr = null;
            while ((tmpStr = reader.readLine()) != null) {
                searchFieldResponse = tmpStr;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return searchFieldResponse;
    }


    /***** Checking FP Device Ends ******/
    public static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{(TrustManager) new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        /* (non-Javadoc)
         * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], java.lang.String)
         */
        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            // TODO Auto-generated method stub

        }
    }};


    public static void turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException, KeyManagementException {
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, UNQUESTIONING_TRUST_MANAGER, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }


}
