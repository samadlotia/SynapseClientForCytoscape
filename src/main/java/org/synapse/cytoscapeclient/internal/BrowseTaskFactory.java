package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.task.read.LoadTableFileTaskFactory;
import org.cytoscape.application.swing.CySwingApplication;

public class BrowseTaskFactory extends AbstractTaskFactory {
  final CySwingApplication cySwingApp;
  final SynClientMgr clientMgr;
  final TaskManager taskMgr;
  final AuthCacheMgr authCacheMgr;

  public BrowseTaskFactory(final CySwingApplication cySwingApp, final SynClientMgr clientMgr, final TaskManager taskMgr, final AuthCacheMgr authCacheMgr) {
    this.cySwingApp = cySwingApp;
    this.clientMgr = clientMgr;
    this.taskMgr = taskMgr;
    this.authCacheMgr = authCacheMgr;
  }

  public TaskIterator createTaskIterator() {
    final TaskIterator iterator = new TaskIterator();
    if (clientMgr.get() == null) {
      iterator.append(new LoginTask(clientMgr, authCacheMgr));
    }
    iterator.append(new BrowseTask(cySwingApp, clientMgr, taskMgr));
    return iterator;
  }

  public boolean isReady() {
    return true;
  }
}