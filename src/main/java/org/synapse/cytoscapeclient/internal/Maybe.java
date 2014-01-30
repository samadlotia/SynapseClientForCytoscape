package org.synapse.cytoscapeclient.internal.nau;

public class Maybe<V> {
  static enum Status {
    SUCCEEDED,
    CANCELLED,
    FAILED
  }

  V           result    = null;
  boolean     cancelled = false;
  Exception   exception = null;

  protected Maybe(final V result, final boolean cancelled, final Exception exception) {
    this.result = result;
    this.cancelled = cancelled;
    this.exception = exception;
  }

  public static <V> Maybe<V> just(final V result) {
    if (result == null)
      throw new NullPointerException("result cannot be null");
    return new Maybe<V>(result, false, null);
  }

  public static <V> Maybe<V> cancelled() {
    return new Maybe<V>(null, true, null);
  }

  public static <V> Maybe<V> failed(final Exception exception) {
    if (exception == null)
      throw new NullPointerException("exception cannot be null");
    return new Maybe<V>(null, false, exception);
  }

  public Status status() {
    if (result != null) {
      return Status.SUCCEEDED;
    } else {
      if (cancelled) {
        return Status.CANCELLED;
      } else {
        return Status.FAILED;
      }
    }
  }

  public V get() throws Exception {
    if (exception != null)
      throw exception;
    return cancelled ? null : result;
  }

  public Exception whyItFailed() {
    return exception;
  }
}