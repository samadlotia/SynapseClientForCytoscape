package org.synapse.cytoscapeclient.internal;

import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromSynapseTaskFactory extends AbstractTaskFactory {
  final CyNetworkManager networkMgr;
  final CyNetworkViewManager networkViewMgr;
  final CyNetworkReaderManager networkReaderMgr;

  public ImportNetworkFromSynapseTaskFactory(final CyNetworkManager networkMgr, final CyNetworkViewManager networkViewMgr, final CyNetworkReaderManager networkReaderMgr) {
    this.networkMgr = networkMgr;
    this.networkViewMgr = networkViewMgr;
    this.networkReaderMgr = networkReaderMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new ImportNetworkFromSynapseTask(networkMgr, networkViewMgr, networkReaderMgr));
  }

  public boolean isReady() {
    return SynapseClient.get() != null;
  }
}