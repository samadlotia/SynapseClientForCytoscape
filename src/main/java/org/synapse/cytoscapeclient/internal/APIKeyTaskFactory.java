package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class APIKeyTaskFactory extends AbstractTaskFactory {
  final APIKeyMgr apiKeyMgr;

  public APIKeyTaskFactory(APIKeyMgr apiKeyMgr) {
    this.apiKeyMgr = apiKeyMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new APIKeyTask(apiKeyMgr));
  }
}

class APIKeyTask implements Task {

  final APIKeyMgr apiKeyMgr;

  @Tunable(description="Synapse API Key")
  public String apiKey;

  public APIKeyTask(APIKeyMgr apiKeyMgr) {
    this.apiKeyMgr = apiKeyMgr;
    this.apiKey = apiKeyMgr.get();
  }

  public void run(TaskMonitor monitor) {
    apiKeyMgr.set(apiKey);
  }

  public void cancel() {}
}