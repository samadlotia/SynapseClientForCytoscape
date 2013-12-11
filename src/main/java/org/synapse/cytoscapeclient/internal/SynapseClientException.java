package org.synapse.cytoscapeclient.internal;

class SynapseClientException extends Exception {
  public SynapseClientException(final String msg) {
    super(msg);
  }

  public SynapseClientException(final String msg, final Throwable t) {
    super(msg, t);
  }
}