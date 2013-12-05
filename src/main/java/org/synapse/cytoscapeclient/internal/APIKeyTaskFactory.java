package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class APIKeyTaskFactory extends AbstractTaskFactory {
  public TaskIterator createTaskIterator() {
    return new TaskIterator();
  }
}
