package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoginTaskFactory extends AbstractTaskFactory {
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;

  public LoginTaskFactory(final SynClientMgr clientMgr, final AuthCacheMgr authCacheMgr) {
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new LoginTask(clientMgr, authCacheMgr));
  }
}
