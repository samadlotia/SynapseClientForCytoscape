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
  final AuthCacheMgr authCacheMgr;

  public ImportNetworkFromSynapseTaskFactory(final CyNetworkManager networkMgr, final CyNetworkViewManager networkViewMgr, final CyNetworkReaderManager networkReaderMgr, final AuthCacheMgr authCacheMgr) {
    this.networkMgr = networkMgr;
    this.networkViewMgr = networkViewMgr;
    this.networkReaderMgr = networkReaderMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new ImportNetworkFromSynapseTask(networkMgr, networkViewMgr, networkReaderMgr, authCacheMgr));
  }

  public boolean isReady() {
    return SynapseClient.get() != null;
  }
}