package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class LoginTaskFactory extends AbstractTaskFactory {
  final AuthCacheMgr authCacheMgr;

  public LoginTaskFactory(final AuthCacheMgr authCacheMgr) {
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new LoginTask(authCacheMgr));
  }
}
