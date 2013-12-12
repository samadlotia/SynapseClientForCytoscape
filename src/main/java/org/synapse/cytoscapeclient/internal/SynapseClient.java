package org.synapse.cytoscapeclient.internal;

import com.fasterxml.jackson.databind.JsonNode;

class SynapseClient {
  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  static SynapseClient client = null;

  final Auth auth;

  JsonNode userProfile = null;

  private SynapseClient(final Auth auth) {
    this.auth = auth;
  }

  public static void loginWithAPIKey(final String userId, final String apiKey) {
    client = new SynapseClient(Auth.withAPIKey(userId, apiKey));
  }

  public static SynapseClient get() {
    return client;
  }

  public String getUserDisplayName() {
    try {
      ensureUserProfile();
      return userProfile.get("displayName").asText();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private void ensureUserProfile() throws RestException {
    if (userProfile != null)
      return;

    userProfile = RestCall.to(REPO_ENDPOINT + "/userProfile/")
      .headers(auth)
      .method("GET")
      .json();
  }
}

