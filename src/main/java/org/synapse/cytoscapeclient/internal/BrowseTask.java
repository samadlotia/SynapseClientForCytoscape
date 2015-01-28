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
import org.cytoscape.task.read.OpenSessionTaskFactory;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * This task opens the Browse dialog.
 * This will ensure that the user has successfully
 * logged into Synapse before showing the Browse dialog.
 */
public class BrowseTask extends AbstractTask {
  final CySwingApplication cySwingApp;
  final SynClientMgr clientMgr;
  final AuthCacheMgr authCacheMgr;
  final TaskManager taskMgr;
  final ImporterMgr importerMgr;
  final LoadNetworkFileTaskFactory loadNetworkFileTF;
  final LoadTableFileTaskFactory loadTableFileTF;
  final OpenSessionTaskFactory openSeshTF;

  public BrowseTask(
        final CySwingApplication cySwingApp,
        final SynClientMgr clientMgr,
        final AuthCacheMgr authCacheMgr,
        final TaskManager taskMgr,
        final ImporterMgr importerMgr,
        final LoadNetworkFileTaskFactory loadNetworkFileTF,
        final LoadTableFileTaskFactory loadTableFileTF,
        final OpenSessionTaskFactory openSeshTF) {
    this.cySwingApp = cySwingApp;
    this.clientMgr = clientMgr;
    this.authCacheMgr = authCacheMgr;
    this.taskMgr = taskMgr;
    this.importerMgr = importerMgr;
    this.loadNetworkFileTF = loadNetworkFileTF;
    this.loadTableFileTF = loadTableFileTF;
    this.openSeshTF = openSeshTF;
  }

  public void run(final TaskMonitor monitor) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (clientMgr.get() == null) {
          startLoginProcess();
        } else {
          showBrowserDialog();
        }
      }
    });
  }

  private void startLoginProcess() {
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
        taskMgr.execute(new TaskIterator(loginTask), new LoginTaskObserver(dialog));
      }
    });
  }

  private void showBrowserDialog() {
    new BrowserDialog(cySwingApp.getJFrame(), clientMgr, taskMgr, importerMgr, loadNetworkFileTF, loadTableFileTF, openSeshTF);
  }

  class LoginTaskObserver implements TaskObserver {
    final LoginDialog dialog;
    public LoginTaskObserver(final LoginDialog dialog) {
      this.dialog = dialog;
    }

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
  }
}
