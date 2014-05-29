package org.synapse.cytoscapeclient.internal;

import java.io.File;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

public class SaveAsTask extends AbstractTask {
  @Tunable(description="Save file from Synapse as", params="input=false;fileCategory=unspecified")
  public File file;

  final SynClient client;
  final SynClient.Entity fileEntity;
  public SaveAsTask(final SynClient client, final SynClient.Entity fileEntity) {
    this.client = client;
    this.fileEntity = fileEntity;
    this.file = new File(fileEntity.getName());
  }

  public void run(TaskMonitor monitor) {
    monitor.setTitle("Save Synapse file to " + file.getName());
    final ResultTask<SynClient.SynFile> fileInfoTask = client.newFileInfoTask(fileEntity.getId());
    super.insertTasksAfterCurrentTask(fileInfoTask, new AbstractTask() {
      public void run(TaskMonitor monitor) {
        final ResultTask<File> downloadTask = client.newDownloadFileTask(fileInfoTask.get(), file);
        super.insertTasksAfterCurrentTask(downloadTask);
      }
    });
  }
}
