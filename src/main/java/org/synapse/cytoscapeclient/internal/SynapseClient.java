package org.synapse.cytoscapeclient.internal;

import com.fasterxml.jackson.databind.JsonNode;

class SynapseClient {
  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  final Auth auth;

  private SynapseClient(Auth auth) {
    this.auth = auth;
  }

  public static SynapseClient loginWithAPIKey(final String userId, final String apiKey) {
    return new SynapseClient(Auth.withAPIKey(userId, apiKey));
  }

  public String getUserDisplayName() {
    try {
      final JsonNode root =
        RestCall.to(REPO_ENDPOINT + "/userProfile/")
                .headers(auth)
                .method("GET")
                .asJson();
      return root.get("displayName").asText();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}

