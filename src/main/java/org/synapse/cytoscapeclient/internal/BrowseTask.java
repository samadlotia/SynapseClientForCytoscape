  package org.synapse.cytoscapeclient.internal;

import javax.swing.SwingUtilities;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadTableFileTaskFactory;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class BrowseTask extends AbstractTask {
  final CySwingApplication cySwingApp;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;
  final TaskManager taskMgr;
  final ImporterMgr importerMgr;
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final LoadTableFileTaskFactory loadTableFileTF;

  public BrowseTask(
        final CySwingApplication cySwingApp,
        final SynClientMgr clientMgr,
        final AuthCacheMgr authCacheMgr,
        final TaskManager taskMgr,
        final ImporterMgr importerMgr,
        final LoadNetworkFileTaskFactory loadNetworkFileTF,
        final LoadTableFileTaskFactory loadTableFileTF) {
    this.cySwingApp = cySwingApp;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
    this.taskMgr = taskMgr;
    this.importerMgr = importerMgr;
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.loadTableFileTF = loadTableFileTF;
  }

  public void run(final TaskMonitor monitor) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (clientMgr.get() == null) {
          final LoginDialog dialog = new LoginDialog(cySwingApp.getJFrame());
          dialog.setFields(authCacheMgr.getUserID(), authCacheMgr.getAPIKey());
          dialog.addOkListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              dialog.disableOkBtn();
              final AbstractTask loginTask = LoginTask.noTunables(
                clientMgr,
                authCacheMgr,
                dialog.getUsername(),
                dialog.getAPIKey());
              taskMgr.execute(new TaskIterator(loginTask), new TaskObserver() {
                public void taskFinished(ObservableTask task) {}

                public void allFinished(final FinishStatus finishStatus) {
                  SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                      if (finishStatus.getType().equals(FinishStatus.Type.SUCCEEDED)) {
                        dialog.close();
                        showBrowserDialog();
                      } else {
                        dialog.reenableOkBtn();
                        dialog.showFailed();
                      }
                    }
                  });
                }
              });
            }
          });
        } else {
          showBrowserDialog();
        }
      }
    });
  }

  private void showBrowserDialog() {
    new BrowserDialog(cySwingApp.getJFrame(), clientMgr, taskMgr, importerMgr, loadNetworkFileTF, loadTableFileTF);
  }

  public void cancel() {
  }
}