package org.synapse.cytoscapeclient.internal;

import java.io.InputStream;
import java.io.IOException;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;

public class ImportNetworkFromSynapseTask extends AbstractTask {
  final CyNetworkManager networkMgr;
  final CyNetworkViewManager networkViewMgr;
  final CyNetworkReaderManager networkReaderMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  volatile InputStream fileContents = null;
  volatile boolean cancelled = false;

  public ImportNetworkFromSynapseTask(final CyNetworkManager networkMgr, final CyNetworkViewManager networkViewMgr, final CyNetworkReaderManager networkReaderMgr) {
    this.networkMgr = networkMgr;
    this.networkViewMgr = networkViewMgr;
    this.networkReaderMgr = networkReaderMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    if (entityId == null || entityId.length() == 0)
      return;

    monitor.setTitle("Import network from Synapse");
    monitor.setStatusMessage("Getting entity information");
    final SynapseClient.File file = SynapseClient.get().getFile(entityId);
    fileContents = file.getContents();

    monitor.setStatusMessage("Reading Synapse file: " + file.getName());
    final CyNetworkReader networkReader = networkReaderMgr.getReader(file.getContents(), file.getName());
    if (networkReader == null)
      throw new Exception("Unsupported network file type: " + file.getName());

    super.insertTasksAfterCurrentTask(networkReader, new AbstractTask() {
      public void run(TaskMonitor monitor) throws Exception {
        for (final CyNetwork network : networkReader.getNetworks()) {
          if (!networkMgr.networkExists(network.getSUID())) {
            networkMgr.addNetwork(network);
          }
          if (!networkViewMgr.viewExists(network)) {
            final CyNetworkView networkView = networkReader.buildCyNetworkView(network);
            networkViewMgr.addNetworkView(networkView);
          }
        }
      }

      public void cancel() {}
    });
  }

  public void cancel() {
    cancelled = true;
    if (fileContents != null) {
      try {
        fileContents.close();
      } catch (IOException e) {}
    }
  }
}