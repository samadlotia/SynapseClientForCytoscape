package org.synapse.cytoscapeclient.internal;

import java.security.InvalidKeyException;

import java.io.InputStream;
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

  public static class File {
    final InputStream contents;
    final String name;

    public File(final InputStream contents, final String name) {
      this.contents = contents;
      this.name = name;
    }

    public InputStream getContents() {
      return contents;
    }

    public String getName() {
      return name;
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
        RestCall.to(REPO_ENDPOINT, "/userProfile/")
        .headers(auth)
        .method("GET")
        .json();
      return userProfile.get("ownerId").asText();
    } catch (RestException e) {
      throw new SynapseClientException("Authentication failed", e);
    }
  }

  public File getFile(final String synapseId) throws SynapseClientException, IOException {
    try {
      // Get info about file to determine type, name, and version
      final JsonNode entityInfo =
        RestCall.to(REPO_ENDPOINT, "/entity/", synapseId, "/bundle?mask=1")
        .headers(auth)
        .method("GET")
        .json();

      // Check entity type
      final String entityType = entityInfo.get("entityType").asText();
      if (!entityType.endsWith("FileEntity"))
        throw new SynapseClientException("Synapse ID does not refer to a file: " + synapseId);

      // Get name and version
      final String name = entityInfo.get("entity").get("name").asText();
      final String version = entityInfo.get("entity").get("versionLabel").asText();

      // Request file
      final InputStream contents =
        RestCall.to(REPO_ENDPOINT, "/entity/", synapseId, "/version/", version, "/file")
        .headers(auth)
        .method("GET")
        .stream();

      return new File(contents, name);
    } catch (RestException e) {
      throw new SynapseClientException("Invalid Synapse ID: " + synapseId, e);
    }
  }
}

