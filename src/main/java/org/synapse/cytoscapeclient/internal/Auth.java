package org.synapse.cytoscapeclient.internal;

import java.util.Date;
import java.util.TimeZone;

import java.text.SimpleDateFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import javax.xml.bind.DatatypeConverter;

abstract class Auth {
  private Auth() {}

  public abstract RestCall authHeaders(RestCall c);

  public static Auth withAPIKey(final String userId, final String apiKey) {
    return new APIKeyAuth(userId, apiKey);
  }

  static class APIKeyAuth extends Auth {
    final String userId;
    final String apiKey;

    public APIKeyAuth(final String userId, final String apiKey) {
      this.userId = userId;
      this.apiKey = apiKey;
    }

    final SimpleDateFormat signatureTimestampFmt = newSignatureTimestampFmt();

    static SimpleDateFormat newSignatureTimestampFmt() {
      final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
      return fmt;
    }

    private String signatureTimestamp() {
      return signatureTimestampFmt.format(new Date());
    }

    public RestCall authHeaders(RestCall c) {
      return c;
    }
  }
}
