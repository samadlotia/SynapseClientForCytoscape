package org.synapse.cytoscapeclient.internal;

import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.session.CyNetworkNaming;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromSynapseTaskFactory extends AbstractTaskFactory {
  final CyNetworkManager networkMgr;
  final CyNetworkViewManager networkViewMgr;
  final CyNetworkReaderManager networkReaderMgr;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;
  final CyNetworkNaming netNaming;

  public ImportNetworkFromSynapseTaskFactory(final CyNetworkManager networkMgr, final CyNetworkViewManager networkViewMgr, final CyNetworkReaderManager networkReaderMgr, final CyNetworkNaming netNaming, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.networkMgr = networkMgr;
    this.networkViewMgr = networkViewMgr;
    this.networkReaderMgr = networkReaderMgr;
    this.netNaming = netNaming;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new ImportNetworkFromSynapseTask(networkMgr, networkViewMgr, networkReaderMgr, netNaming, clientMgr, authCacheMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}
