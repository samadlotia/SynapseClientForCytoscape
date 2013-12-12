package org.synapse.cytoscapeclient.internal;

import java.security.InvalidKeyException;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

class SynapseClient {
  static SynapseClient client = null;

  public static SynapseClient get() {
    return client;
  }

  public static void loginWithAPIKey(final String userId, final String apiKey) throws SynapseClientException, IOException {
    try {
      client = new SynapseClient(Auth.withAPIKey(userId, apiKey));
    } catch (InvalidKeyException e) {
      throw new SynapseClientException("Invalid API key specified", e);
    }
  }

  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  final Auth auth;
  final String userId;

  private SynapseClient(final Auth auth) throws SynapseClientException, IOException {
    this.auth = auth;
    this.userId = authenticateAndGetUserId();
  }

  private String authenticateAndGetUserId() throws SynapseClientException, IOException {
    try {
      final JsonNode userProfile =
        RestCall.to("%s%s", REPO_ENDPOINT, "/userProfile/")
        .headers(auth)
        .method("GET")
        .json();
      return userProfile.get("ownerId").asText();
    } catch (RestException e) {
      throw new SynapseClientException("Authentication failed", e);
    }
  }
}

