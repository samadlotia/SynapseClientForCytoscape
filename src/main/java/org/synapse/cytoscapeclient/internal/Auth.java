package org.synapse.cytoscapeclient.internal;

import java.util.Date;
import java.util.TimeZone;

import java.text.SimpleDateFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import javax.xml.bind.DatatypeConverter;

abstract class Auth implements RestCall.HeaderAdder {
  private Auth() {}

  /**
   * Add authentication headers to the given rest call.
   */
  public abstract void headers(RestCall c);

  public static Auth withAPIKey(final String userId, final String apiKey) {
    return new APIKeyAuth(userId, apiKey);
  }

  static class APIKeyAuth extends Auth {
    final static String CRYPT_ALGO = "HmacSHA1";

    final SimpleDateFormat signatureTimestampFmt = newSignatureTimestampFmt();
    final String userId;
    final Mac mac;

    public APIKeyAuth(final String userId, final String apiKey) {
      this.userId = userId;

      // initialize mac with the given api key as its secret key
      final byte[] keyBytes = DatatypeConverter.parseBase64Binary(apiKey);
      try {
        final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, CRYPT_ALGO);
        mac = Mac.getInstance(CRYPT_ALGO);
        mac.init(keySpec);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("CRYPT_ALGO is invalid: " + CRYPT_ALGO, e);
      } catch (InvalidKeyException e) {
        throw new IllegalArgumentException("apiKey is invalid" + apiKey, e);
      }
    }

    static SimpleDateFormat newSignatureTimestampFmt() {
      final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
      return fmt;
    }

    private String signatureTimestamp() {
      return signatureTimestampFmt.format(new Date());
    }

    private String encrypt(final String plainMsg) {
      final byte[] encryptedMsg = mac.doFinal(plainMsg.getBytes());
      return DatatypeConverter.printBase64Binary(encryptedMsg);
    }

    public void headers(final RestCall c) {
      final String path = c.getConnection().getURL().getPath();
      final String timestamp = signatureTimestamp();
      final String signature = userId + path + timestamp;

      c.header("userId",             userId)
       .header("signatureTimestamp", timestamp)
       .header("signature",          encrypt(signature));
    }
  }
}
