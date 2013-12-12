package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.Task;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class ImportNetworkFromSynapseTask implements Task {
  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  @Tunable(description="Version", gravity=2.0)
  public String version = "1";

  public ImportNetworkFromSynapseTask() {
  }

  public void run(TaskMonitor monitor) throws Exception {
  }

  public void cancel() {}
}