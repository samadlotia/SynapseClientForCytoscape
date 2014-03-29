package org.synapse.cytoscapeclient.internal;

import java.util.List;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import org.cytoscape.work.TaskManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

class BrowserDialog {
  final SynClient client;
  final TaskManager taskMgr;
  final JEditorPane infoPane;
  final JDialog dialog;
  final DefaultTreeModel model;
  final JTree tree;

  public BrowserDialog(final Frame parent, final SynClientMgr clientMgr, final TaskManager taskMgr) {
    client = clientMgr.get();
    this.taskMgr = taskMgr;
    dialog = new JDialog(parent, "Browse Synapse", false);
    model = new DefaultTreeModel(new DefaultMutableTreeNode());
    infoPane = new JEditorPane("text/html", "");
    tree = new JTree(model);

    final JButton importNetworkBtn = new JButton("Import as Network");
    importNetworkBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        final SynClient.Entity entity = getSelectedEntity();
        taskMgr.execute(new TaskIterator(ImportNetworkFromSynapseTask.noTunables(null, clientMgr, entity.getId())));
      }
    });
    final JButton importTableBtn = new JButton("Import as Table");

    tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        final SynClient.Entity entity = getSelectedEntity();
        if (entity == null) {
          infoPane.setText("");
        } else {
          infoPane.setText(String.format("<html><h1>%s</h1><h3>%s</h3></html>", entity.getName(), entity.getId()));
        }

        if (entity != null && entity.getType().endsWith("FileEntity")) {
          importNetworkBtn.setEnabled(true);
          importTableBtn.setEnabled(true);
        } else {
          importNetworkBtn.setEnabled(false);
          importTableBtn.setEnabled(false);
        }
      }
    });

    final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    buttonsPanel.add(importNetworkBtn);
    buttonsPanel.add(importTableBtn);

    final JPanel secondaryPanel = new JPanel(new GridBagLayout());
    final EasyGBC e = new EasyGBC();
    secondaryPanel.add(new JScrollPane(infoPane), e.expandHV());
    secondaryPanel.add(buttonsPanel, e.expandH().down());

    dialog.setLayout(new GridBagLayout());
    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), secondaryPanel);
    splitPane.setResizeWeight(0.5);
    dialog.add(splitPane, e.reset().expandHV());

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

  private SynClient.Entity getSelectedEntity() {
    final TreePath path = tree.getSelectionPath();
    if (path == null) {
      return null;
    }
    final Object selection = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    if (!(selection instanceof SynClient.Entity)) {
      return null;
    }
    return (SynClient.Entity) selection;
  }

  class AddProjects extends AbstractTask {
    final ResultTask<SynClient.UserProfile> userProfileTask;
    final ResultTask<List<SynClient.Entity>> projectsTask;

    public AddProjects(final ResultTask<SynClient.UserProfile> userProfileTask, final ResultTask<List<SynClient.Entity>> projectsTask) {
      this.userProfileTask = userProfileTask;
      this.projectsTask = projectsTask;
    }

    public void run(final TaskMonitor monitor) {
      final List<SynClient.Entity> projects = projectsTask.get();
      if (projects == null)
        return;
      final DefaultMutableTreeNode root = new DefaultMutableTreeNode(userProfileTask.get().getUserName());
      for (final SynClient.Entity project : projects) {
        final DefaultMutableTreeNode projectNode = new DefaultMutableTreeNode(project);
        root.add(projectNode);
        final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(project.getId());
        super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, projectNode, project.getId()));
      }
      model.setRoot(root);
    }

    public void cancel() {}
  }

  static boolean hasChildren(final SynClient.Entity entity) {
    final String type = entity.getType();
    return type.endsWith("Folder");
  }

  class AddChildren extends AbstractTask {
    final ResultTask<List<SynClient.Entity>> task;
    final DefaultMutableTreeNode parentNode;
    final String parentId;

    public AddChildren(final ResultTask<List<SynClient.Entity>> task, final DefaultMutableTreeNode parentNode, final String parentId) {
      this.task = task;
      this.parentNode = parentNode;
      this.parentId = parentId;
    }

    public void run(final TaskMonitor monitor) {
      final List<SynClient.Entity> children = task.get();
      for (final SynClient.Entity child : children) {
        final DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
        parentNode.add(childNode);

        if (hasChildren(child)) {
          final ResultTask<List<SynClient.Entity>> childrenTask = client.newChildrenTask(child.getId());
          super.insertTasksAfterCurrentTask(childrenTask, new AddChildren(childrenTask, childNode, child.getId()));
        }
      }
      tree.expandPath(new TreePath(parentNode.getPath()));
    }

    public void cancel() {}
  }
}