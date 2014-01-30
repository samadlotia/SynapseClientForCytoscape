package org.synapse.cytoscapeclient.internal;

import org.cytoscape.model.CyTableManager;
import org.cytoscape.io.read.CyTableReaderManager;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportTableFromSynapseTaskFactory extends AbstractTaskFactory {
  final CyTableManager tableMgr;
  final CyTableReaderManager tableReaderMgr;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  public ImportTableFromSynapseTaskFactory(final CyTableManager tableMgr, final CyTableReaderManager tableReaderMgr, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.tableMgr = tableMgr;
    this.tableReaderMgr = tableReaderMgr;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new ImportTableFromSynapseTask(tableMgr, tableReaderMgr, clientMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}