package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromSynapseTaskFactory extends AbstractTaskFactory {
  public ImportNetworkFromSynapseTaskFactory() {

  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new ImportNetworkFromSynapseTask());
  }

  public boolean isReady() {
    return SynapseClient.get() != null;
  }
}