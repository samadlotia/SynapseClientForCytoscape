package org.synapse.cytoscapeclient.internal;

import java.util.List;

import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class BrowserDialog {
  final SynClient client;
  final TaskManager taskMgr;
  final JDialog dialog;
  final JTree tree;

  public BrowserDialog(final Frame parent, final SynClientMgr clientMgr, final TaskManager taskMgr) {
    client = clientMgr.get();
    this.taskMgr = taskMgr;
    dialog = new JDialog(parent, "Browse Synapse", false);
    tree = new JTree();

    dialog.setLayout(new GridLayout(0, 1));
    dialog.add(new JScrollPane(tree));

    dialog.pack();
    dialog.setVisible(true);

    final ResultTask<SynClient.UserProfile> userProfileTask = client.newUserProfileTask();
    taskMgr.execute(new TaskIterator(userProfileTask, new AbstractTask() {
      public void run(TaskMonitor monitor) {
        final ResultTask<List<SynClient.Entity>> projectsTask = client.newProjectsTask(userProfileTask.get());
        super.insertTasksAfterCurrentTask(projectsTask, new AddProjects(userProfileTask, projectsTask));
      }

      public void cancel() {}
    }));
  }

  class AddProjects extends AbstractTask {
    final ResultTask<SynClient.UserProfile> userProfileTask;
    final ResultTask<List<SynClient.Entity>> projectsTask;

    public AddProjects(final ResultTask<SynClient.UserProfile> userProfileTask, final ResultTask<List<SynClient.Entity>> projectsTask) {
      this.userProfileTask = userProfileTask;
      this.projectsTask = projectsTask;
    }

    public void run(final TaskMonitor monitor) throws Exception {
      final List<SynClient.Entity> projects = projectsTask.get();
      if (projects == null)
        return;
      final DefaultMutableTreeNode root = new DefaultMutableTreeNode(userProfileTask.get().getUserName());
      for (final SynClient.Entity project : projects) {
        final DefaultMutableTreeNode child = new DefaultMutableTreeNode(project.getName());
        root.add(child);
      }
      ((DefaultTreeModel) tree.getModel()).setRoot(root);
    }

    public void cancel() {}
  }
}