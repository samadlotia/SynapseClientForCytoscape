package org.synapse.cytoscapeclient.internal;

import java.io.File;
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
    final SynapseClient.SynFile file = SynapseClient.get().getFile(entityId);

    monitor.setStatusMessage("Reading Synapse file: " + file.name);
    final CyNetworkReader networkReader = networkReaderMgr.getReader(file.file.toURI(), file.name);
    if (networkReader == null)
      throw new Exception("Unsupported network file type: " + file.name);

    super.insertTasksAfterCurrentTask(networkReader, new AbstractTask() {
      volatile boolean cancelled = false;
      public void run(TaskMonitor monitor) throws Exception {
        for (final CyNetwork network : networkReader.getNetworks()) {
          if (cancelled)
            break;
          
          if (!networkMgr.networkExists(network.getSUID())) {
            networkMgr.addNetwork(network);
          }
          if (!networkViewMgr.viewExists(network)) {
            final CyNetworkView networkView = networkReader.buildCyNetworkView(network);
            networkViewMgr.addNetworkView(networkView);
          }
        }
      }

      public void cancel() {
        cancelled = true;
      }
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