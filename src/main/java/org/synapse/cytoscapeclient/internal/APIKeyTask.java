package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class APIKeyTask implements Task {
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