package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.task.read.OpenSessionTaskFactory;

public class OpenSessionFromSynapseTaskFactory extends AbstractTaskFactory {
  final OpenSessionTaskFactory openSeshTF;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  public OpenSessionFromSynapseTaskFactory(final OpenSessionTaskFactory openSeshTF, final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.openSeshTF = openSeshTF;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new OpenSessionFromSynapseTask(openSeshTF, clientMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}
