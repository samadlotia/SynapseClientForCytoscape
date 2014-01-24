package org.synapse.cytoscapeclient.internal;

import java.security.InvalidKeyException;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

class SynapseClient {
  static SynapseClient client = null;
  static final int BLOCK_SIZE = 64 * 1024;

  static class SynFile {
    public String name;
    public File file;

    public SynFile(String name, File file) {
      this.name = name;
      this.file = file;
    }
  }

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
        RestCall.to(REPO_ENDPOINT, "/userProfile/")
        .headers(auth)
        .method("GET")
        .json();
      return userProfile.get("ownerId").asText();
    } catch (RestException e) {
      throw new SynapseClientException("Authentication failed", e);
    }
  }

  public SynFile getFile(final String synapseId) throws SynapseClientException, IOException {
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
      final InputStream input =
        RestCall.to(REPO_ENDPOINT, "/entity/", synapseId, "/version/", version, "/file")
        .headers(auth)
        .method("GET")
        .stream();

      final File file = newTempFile(name);
      final FileOutputStream output = new FileOutputStream(file);
      final byte[] buffer = new byte[BLOCK_SIZE];
      while (true) {
        final int len = input.read(buffer);
        if (len < 0)
          break;
        output.write(buffer, 0, len);
      }
      output.close();
      input.close();

      return new SynFile(name, file);
    } catch (RestException e) {
      throw new SynapseClientException("Invalid Synapse ID: " + synapseId, e);
    }
  }

  static File newTempFile(final String fullname) throws IOException {
    final int exti = fullname.lastIndexOf('.');
    if (exti < 0) {
      return File.createTempFile(fullname, null);
    } else {
      final String name = fullname.substring(0, exti);
      final String ext = fullname.substring(exti);
      return File.createTempFile(name, ext);
    }
  }
}

