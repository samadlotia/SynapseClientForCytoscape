package org.synapse.cytoscapeclient.internal;

import org.cytoscape.task.read.LoadNetworkFileTaskFactory;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromSynapseTaskFactory extends AbstractTaskFactory {
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  public ImportNetworkFromSynapseTaskFactory(/*final CyNetworkManager networkMgr, final CyNetworkViewManager networkViewMgr, final CyNetworkReaderManager networkReaderMgr, final CyNetworkNaming netNaming, */ final LoadNetworkFileTaskFactory loadNetworkFileTF, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new ImportNetworkFromSynapseTask(loadNetworkFileTF, clientMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}
