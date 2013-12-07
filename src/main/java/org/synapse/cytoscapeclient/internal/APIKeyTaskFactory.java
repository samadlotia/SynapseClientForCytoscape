package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class APIKeyTaskFactory extends AbstractTaskFactory {
  final APIKeyMgr apiKeyMgr;

  public APIKeyTaskFactory(APIKeyMgr apiKeyMgr) {
    this.apiKeyMgr = apiKeyMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new APIKeyTask(apiKeyMgr));
  }
}
