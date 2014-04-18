package org.synapse.cytoscapeclient.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class AsyncTaskMgr {
  final BrowserDialog dialog;
  final ExecutorService service;
  volatile TaskRunner runner = null;

  public AsyncTaskMgr(final BrowserDialog dialog) {
    this.dialog = dialog;
    this.service = Executors.newFixedThreadPool(4);
  }

  public void execute(final TaskIterator iterator) {
    final TaskRunner runner2 = runner;
    if (runner2 != null) {
      runner2.cancel();
    }
    runner = new TaskRunner(iterator);
    service.submit(runner);
  }

  class TaskRunner implements Runnable {
    final TaskIterator iterator;
    volatile Task task = null;
    volatile boolean cancelled = false;

    public TaskRunner(final TaskIterator iterator) {
      this.iterator = iterator;
    }

    public void run() {
      final TaskMonitor monitor = new InternalTaskMonitor();
      while (iterator.hasNext() && !cancelled) {
        task = iterator.next();
        try {
          task.run(monitor);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      dialog.loadingDone();
      runner = null;
    }

    public void cancel() {
      cancelled = true;
      final Task task2 = task;
      if (task2 != null) {
        task2.cancel();
      }
    }
  }

  class InternalTaskMonitor implements TaskMonitor {
    public void setProgress(double progress) {}
    public void setStatusMessage(String statusMessage) {}
    public void showMessage(TaskMonitor.Level level, String message) {}

    public void setTitle(String title) {
      dialog.setLoadingText(title);
    }
  }
}