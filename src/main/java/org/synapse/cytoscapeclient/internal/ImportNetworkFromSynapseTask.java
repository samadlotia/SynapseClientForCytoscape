package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class ImportNetworkFromSynapseTask implements Task {
  @Tunable(description="Entity ID")
  public String entityId;

  public ImportNetworkFromSynapseTask() {
  }

  public void run(TaskMonitor monitor) throws Exception {
  }

  public void cancel() {}
}