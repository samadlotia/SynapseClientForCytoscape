package org.synapse.cytoscapeclient.internal; 

import java.util.Date;
import java.util.TimeZone;

import java.text.SimpleDateFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import org.apache.commons.codec.binary.Base64;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

/**
 * Adds authentication headers to http requests made to the Synapse web service.
 * The Synapse web service requires authenticated http requests to have {@code userId},
 * {@code signatureTimestamp}, and {@code signature} http headers. This
 * class adds these headers.
 */
public class APIKeyAuth implements HttpRequestInterceptor {
  final static String CRYPT_ALGO = "HmacSHA1";

  static SimpleDateFormat newSignatureTimestampFmt() {
    final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt;
  }

  final SimpleDateFormat signatureTimestampFmt = newSignatureTimestampFmt();
  final String userId;
  final Mac mac;

  public APIKeyAuth(final String userId, final String apiKey) {
    this.userId = userId;

    // convert apiKey base64 string into a byte array
    final byte[] keyBytes = Base64.decodeBase64(apiKey);

    // initialize mac with the given api key as its secret key
    try {
      final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, CRYPT_ALGO);
      mac = Mac.getInstance(CRYPT_ALGO);
      mac.init(keySpec);
    } catch (InvalidKeyException e) {
      throw new IllegalArgumentException("Invalid key: " + apiKey, e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm defined in CRYPT_ALGO is invalid: " + CRYPT_ALGO, e);
    }
  }

  private String signatureTimestamp() {
    return signatureTimestampFmt.format(new Date());
  }

  private String encrypt(final String plainMsg) {
    final byte[] encryptedMsg = mac.doFinal(plainMsg.getBytes());
    return Base64.encodeBase64String(encryptedMsg);
  }

  public void process(final HttpRequest request, final HttpContext context) {
    final HttpUriRequest uriRequest = (HttpUriRequest) request;

    final String path = uriRequest.getURI().getPath();
    final String timestamp = signatureTimestamp();
    final String signature = userId + path + timestamp;

    request.addHeader("userId",             userId);
    request.addHeader("signatureTimestamp", timestamp);
    request.addHeader("signature",          encrypt(signature));
  }
}
