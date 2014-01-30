package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.TaskMonitor;

public abstract class MaybeTask<T> {
  protected volatile boolean cancelled = false;

  protected abstract T checkedRun(TaskMonitor monitor) throws Exception;
  protected abstract void innerCancel();

  public Maybe<T> run(TaskMonitor monitor) {
    try {
      final T result = checkedRun(monitor);
      if (cancelled || result == null) {
        return Maybe.cancelled();
      } else {
        return Maybe.just(result);
      }
    } catch (Exception e) {
      return Maybe.failed(e);
    }
  }

  public void cancel() {
    cancelled = true;
    innerCancel();
  }
}
