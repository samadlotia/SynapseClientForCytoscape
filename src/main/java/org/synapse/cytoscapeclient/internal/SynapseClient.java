package org.synapse.cytoscapeclient.internal;

class SynapseClient {
  static final String AUTH_ENDPOINT = "https://auth-prod.prod.sagebase.org/auth/v1";
  static final String REPO_ENDPOINT = "https://repo-prod.prod.sagebase.org/repo/v1";

  final Auth auth;

  public SynapseClient(Auth auth) {
    this.auth = auth;
  }
}

