package org.synapse.cytoscapeclient.internal;

import javax.swing.SwingUtilities;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;

public class BrowseTask extends AbstractTask {
  final CySwingApplication cySwingApp;
  final SynClientMgr clientMgr;
  final TaskManager taskMgr;
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final LoadTableFileTaskFactory loadTableFileTF;

  public BrowseTask(final CySwingApplication cySwingApp, final SynClientMgr clientMgr, final TaskManager taskMgr, final LoadNetworkFileTaskFactory loadNetworkFileTF, final LoadTableFileTaskFactory loadTableFileTF) {
    this.cySwingApp = cySwingApp;
    this.clientMgr = clientMgr;
    this.taskMgr = taskMgr;
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.loadTableFileTF = loadTableFileTF;
  }

  public void run(final TaskMonitor monitor) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new BrowserDialog(cySwingApp.getJFrame(), clientMgr, taskMgr, loadNetworkFileTF, loadTableFileTF);
      }
    });
  }

  public void cancel() {
  }
}