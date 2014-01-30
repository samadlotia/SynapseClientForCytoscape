package org.synapse.cytoscapeclient.internal.nau;

class SynClientException extends Exception {
  public SynClientException(final String msg) {
    super(msg);
  }

  public SynClientException(final String msg, final Throwable t) {
    super(msg, t);
  }
}