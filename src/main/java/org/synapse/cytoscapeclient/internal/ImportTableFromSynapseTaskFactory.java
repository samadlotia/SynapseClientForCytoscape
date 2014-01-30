package org.synapse.cytoscapeclient.internal;

import org.cytoscape.io.read.CyTableReaderManager;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportTableFromSynapseTaskFactory extends AbstractTaskFactory {
  final CyTableReaderManager tableReaderMgr;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  public ImportTableFromSynapseTaskFactory(final CyTableReaderManager tableReaderMgr, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.tableReaderMgr = tableReaderMgr;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new ImportTableFromSynapseTask(clientMgr, tableReaderMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}