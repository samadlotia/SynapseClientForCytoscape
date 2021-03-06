package org.synapse.cytoscapeclient.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 * Executes tasks asynchronously. 
 *
 * This is to be used only executing Synapse web query tasks.
 *
 * Acts as a replacement for {@link org.cytoscape.work.TaskManager}
 * where task execution does not block the UI.
 *
 * This updates {@code BrowserDialog} UI according to the status of
 * task execution. Task titles are shown at the bottom of the
 * {@code BrowserDialog}. If any task is currently executing,
 * a "loading" icon is shown at the bottom of the task as well.
 * If all tasks have finished executing, the "loading" icon is closed.
 */
class AsyncTaskMgr {
  public static final int N_THREADS = 16; // HttpClient needs to know this value
  final BrowserDialog dialog;
  final ExecutorService service;
  final AtomicInteger activeTasks;

  public AsyncTaskMgr(final BrowserDialog dialog) {
    this.dialog = dialog;
    this.service = Executors.newFixedThreadPool(N_THREADS, new ThreadFactory() {
      int count = 0;
      public Thread newThread(final Runnable r) {
        return new Thread(r, String.format("AsyncTaskMgr-Thread-%d-Factory-0x%x", count++, AsyncTaskMgr.this.hashCode()));
      }
    });
    this.activeTasks = new AtomicInteger(0);
  }

  public void execute(final TaskIterator iterator) {
    final Runnable runner = new TaskRunner(iterator);
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
      activeTasks.incrementAndGet();
      final TaskMonitor monitor = new InternalTaskMonitor();
      while (iterator.hasNext() && !cancelled) {
        task = iterator.next();
        try {
          task.run(monitor);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (activeTasks.decrementAndGet() == 0) {
        dialog.loadingDone();
      }
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
