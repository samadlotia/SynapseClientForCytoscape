package org.synapse.cytoscapeclient.internal;

public class SynClientMgr {
  SynClient client = null;

  public SynClient get() {
    return client;
  }

  public void set(SynClient client) {
    this.client = client;
  }
}