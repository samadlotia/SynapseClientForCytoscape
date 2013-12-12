package org.synapse.cytoscapeclient.internal;

import java.security.InvalidKeyException;

import com.fasterxml.jackson.databind.JsonNode;

class SynapseClient {
  static SynapseClient client = null;

  public static SynapseClient get() {
    return client;
  }

  public static void loginWithAPIKey(final String userId, final String apiKey) throws SynapseClientException {
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

  private SynapseClient(final Auth auth) throws SynapseClientException {
    this.auth = auth;
    this.userId = authenticateAndGetUserId();
  }

  private String authenticateAndGetUserId() throws SynapseClientException {
    try {
      final JsonNode userProfile =
        RestCall.to("%s%s", REPO_ENDPOINT, "/userProfile/")
        .headers(auth)
        .method("GET")
        .json();
      return userProfile.get("ownerId").asText();
    } catch (RestException e) {
      if (e.getCode() == 0) {
        throw new SynapseClientException("Unable to connect to Synapse server", e);
      } else {
        throw new SynapseClientException("Authentication failed", e);
      }
    }
  }
}

