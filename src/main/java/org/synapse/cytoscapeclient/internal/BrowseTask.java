package org.synapse.cytoscapeclient.internal;

import javax.swing.SwingUtilities;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskManager;
import org.cytoscape.application.swing.CySwingApplication;

public class BrowseTask extends AbstractTask {
  final CySwingApplication cySwingApp;
  final SynClientMgr clientMgr;
  final TaskManager taskMgr;

  public BrowseTask(final CySwingApplication cySwingApp, final SynClientMgr clientMgr, final TaskManager taskMgr) {
    this.cySwingApp = cySwingApp;
    this.clientMgr = clientMgr;
    this.taskMgr = taskMgr;
  }

  public void run(final TaskMonitor monitor) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new BrowserDialog(cySwingApp.getJFrame(), clientMgr, taskMgr);
      }
    });
  }

  public void cancel() {
  }
}