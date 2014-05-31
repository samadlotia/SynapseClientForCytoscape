package org.synapse.cytoscapeclient.internal;

import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.task.read.LoadTableFileTaskFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;

public class BrowseTaskFactory extends AbstractTaskFactory {
  final CySwingApplication cySwingApp;
  final SynClientMgr clientMgr;
  final TaskManager taskMgr;
  final AuthCacheMgr authCacheMgr;
  final ImporterMgr importerMgr;
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final LoadTableFileTaskFactory loadTableFileTF;

  public BrowseTaskFactory(
        final CySwingApplication cySwingApp,
        final SynClientMgr clientMgr,
        final TaskManager taskMgr,
        final AuthCacheMgr authCacheMgr,
        final ImporterMgr importerMgr,
        final LoadNetworkFileTaskFactory loadNetworkFileTF,
        final LoadTableFileTaskFactory loadTableFileTF) {
    this.cySwingApp = cySwingApp;
    this.clientMgr = clientMgr;
    this.taskMgr = taskMgr;
    this.authCacheMgr = authCacheMgr;
    this.importerMgr = importerMgr;
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.loadTableFileTF = loadTableFileTF;
  }

  public TaskIterator createTaskIterator() {
    return new TaskIterator(new BrowseTask(cySwingApp, clientMgr, authCacheMgr, taskMgr, importerMgr, loadNetworkFileTF, loadTableFileTF));
  }

  public boolean isReady() {
    return true;
  }
}