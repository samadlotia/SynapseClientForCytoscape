package org.synapse.cytoscapeclient.internal;

import java.io.IOException;

/**
 * Exception class used by {@code RestCall}.
 * This ought to be wrapped in a higher level exception.
 */
class RestException extends Exception {
  final int code;
  final String msg;
  final String response;

  public RestException(final int code, final String msg, final String response) {
    this.code = code;
    this.msg = msg;
    this.response = response;
  }

  public RestException(final String msg) {
    super(msg);
    this.code = 0;
    this.msg = null;
    this.response = null;
  }

  public RestException(final String msg, final Throwable t) {
    super(msg, t);
    this.code = 0;
    this.msg = null;
    this.response = null;
  }

  public RestException(final IOException e) {
    this("Low-level I/O exception with server", e);
  }

  public String toString() {
    if (msg != null)
      return String.format("%d - %s: %s", code, msg, response);
    else
      return super.toString();
  }
}

