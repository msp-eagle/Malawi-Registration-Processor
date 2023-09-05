package io.mosip.registration.processor.stages.uingenerator.stage;


import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


public class SslOff {

    public static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[] { new X509TrustManager() {
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
    } };


    public  RestTemplate turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        TrustStrategy acceptingTrustStrategy = (x509Certificates, s) -> true;
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;

    }

    public static void sslOff() throws KeyManagementException, NoSuchAlgorithmException {
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
