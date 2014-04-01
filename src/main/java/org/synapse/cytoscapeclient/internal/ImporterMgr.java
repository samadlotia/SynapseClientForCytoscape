package org.synapse.cytoscapeclient.internal;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;

public class ImporterMgr {
  final Map<InputStreamTaskFactory,CyFileFilter> fileFilters = new HashMap<InputStreamTaskFactory,CyFileFilter>();

  public void addFactory(final InputStreamTaskFactory factory, Map<?,?> props) {
    fileFilters.put(factory, factory.getFileFilter());
  }

  public void removeFactory(final InputStreamTaskFactory factory, Map<?,?> props) {
    fileFilters.remove(factory);
  }

  public boolean doesImporterExist(final String extension, final DataCategory category) {
    System.out.println("doesImporterExist: " + extension + " for " + category.getDisplayName());
    final Set<CyFileFilter> fileFilters2 = new HashSet<CyFileFilter>(fileFilters.values()); // copy fileFilters to prevent concurrent modifications while looping thru fileFilters
    for (final CyFileFilter fileFilter : fileFilters2) {
      System.out.println("Checking data category: " + fileFilter.getDataCategory().getDisplayName());
      if (!fileFilter.getDataCategory().equals(category)) {
        continue;
      }
      System.out.println("Checking extensions: " + fileFilter.getExtensions());
      if (fileFilter.getExtensions().contains(extension)) {
        return true;
      }
    }
    return false;
  }
}