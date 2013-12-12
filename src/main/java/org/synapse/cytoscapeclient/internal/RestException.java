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

  public String toString() {
    return String.format("%d - %s: %s", code, msg, response);
  }
}

