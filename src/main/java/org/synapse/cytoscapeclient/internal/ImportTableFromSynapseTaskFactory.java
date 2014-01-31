package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.task.read.LoadTableFileTaskFactory;

public class ImportTableFromSynapseTaskFactory extends AbstractTaskFactory {
  final LoadTableFileTaskFactory loadTableFileTF;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  public ImportTableFromSynapseTaskFactory(final LoadTableFileTaskFactory loadTableFileTF, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.loadTableFileTF = loadTableFileTF;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new ImportTableFromSynapseTask(loadTableFileTF, clientMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}