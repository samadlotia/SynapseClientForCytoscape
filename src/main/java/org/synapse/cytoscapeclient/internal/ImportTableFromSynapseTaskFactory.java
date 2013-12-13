package org.synapse.cytoscapeclient.internal;

import org.cytoscape.io.read.CyTableReaderManager;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportTableFromSynapseTaskFactory extends AbstractTaskFactory {
  final CyTableReaderManager tableReaderMgr;

  public ImportTableFromSynapseTaskFactory(final CyTableReaderManager tableReaderMgr) {
    this.tableReaderMgr = tableReaderMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new ImportTableFromSynapseTask(tableReaderMgr));
  }

  public boolean isReady() {
    return SynapseClient.get() != null;
  }
}