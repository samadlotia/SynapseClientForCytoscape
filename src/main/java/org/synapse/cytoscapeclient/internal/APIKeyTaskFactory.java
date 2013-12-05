package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class APIKeyTaskFactory extends AbstractTaskFactory {
  public TaskIterator createTaskIterator() {
    return new TaskIterator(new APIKeyTask());
  }
}

class APIKeyTask implements Task {
  @Tunable(description="Synapse API Key")
  public String apiKey;

  public void run(TaskMonitor monitor) {
    System.out.println(apiKey);
  }

  public void cancel() {}
}