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
import org.cytoscape.session.CyNetworkNaming;

public class ImportNetworkFromSynapseTask extends AbstractTask {
  final CyNetworkManager networkMgr;
  final CyNetworkViewManager networkViewMgr;
  final CyNetworkReaderManager networkReaderMgr;
  final CyNetworkNaming netNaming;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  @Tunable(description="Synapse ID", gravity=1.0)
  public String entityId;

  volatile InputStream fileContents = null;
  volatile boolean cancelled = false;

  public ImportNetworkFromSynapseTask(final CyNetworkManager networkMgr, final CyNetworkViewManager networkViewMgr, final CyNetworkReaderManager networkReaderMgr, final CyNetworkNaming netNaming, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.networkMgr = networkMgr;
    this.networkViewMgr = networkViewMgr;
    this.networkReaderMgr = networkReaderMgr;
    this.netNaming = netNaming;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public void run(TaskMonitor monitor) throws Exception {
    if (entityId == null || entityId.length() == 0)
      return;

    final SynClient client = clientMgr.get();
    if (client == null) { // not logged in, so just exit
      return;
    }

    monitor.setTitle("Import network from Synapse");
    final SynClient.SynFile file = client.newGetFileTask(entityId).run(monitor).get();
    if (file == null) { // user cancelled, so exit
      return;
    }

    monitor.setStatusMessage("Reading Synapse file: " + file.getName());
    final CyNetworkReader networkReader = networkReaderMgr.getReader(file.getFile().toURI(), file.getName());
    if (networkReader == null)
      throw new Exception("Unsupported network file type: " + file.getName());

    super.insertTasksAfterCurrentTask(networkReader, new AbstractTask() {
      volatile boolean cancelled = false;
      public void run(TaskMonitor monitor) throws Exception {
        for (final CyNetwork network : networkReader.getNetworks()) {
          network.getRow(network).set(CyNetwork.NAME, netNaming.getSuggestedNetworkTitle(file.getName()));

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
