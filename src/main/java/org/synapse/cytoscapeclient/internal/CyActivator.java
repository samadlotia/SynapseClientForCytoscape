package org.synapse.cytoscapeclient.internal;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.io.read.CyTableReaderManager;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.ServiceProperties;

public class CyActivator extends AbstractCyActivator {
  public void start(BundleContext bc) {
    System.out.println((new java.util.Date()).toString() + " started: " + getClass().getName());
    
    final CyNetworkManager networkMgr = getService(bc, CyNetworkManager.class);
    final CyNetworkViewManager networkViewMgr = getService(bc, CyNetworkViewManager.class);
    final CyNetworkReaderManager networkReaderMgr = getService(bc, CyNetworkReaderManager.class);
    final CyTableManager tableMgr = getService(bc, CyTableManager.class);
    final CyTableReaderManager tableReaderMgr = getService(bc, CyTableReaderManager.class);
    final CyApplicationConfiguration cyAppConf = getService(bc, CyApplicationConfiguration.class);

    final SynClientMgr clientMgr = new SynClientMgr();
    final AuthCacheMgr authCacheMgr = new AuthCacheMgr(cyAppConf.getAppConfigurationDirectoryLocation(this.getClass()));

    registerService(bc, new LoginTaskFactory(clientMgr, authCacheMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "Login...",
      ServiceProperties.PREFERRED_MENU, "Apps.Synapse"
    ));

    registerService(bc, new ImportNetworkFromSynapseTaskFactory(networkMgr, networkViewMgr, networkReaderMgr, clientMgr, authCacheMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "From Synapse...",
      ServiceProperties.PREFERRED_MENU, "File.Import.Network"
    ));

    registerService(bc, new ImportTableFromSynapseTaskFactory(tableMgr, tableReaderMgr, clientMgr, authCacheMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "From Synapse...",
      ServiceProperties.PREFERRED_MENU, "File.Import.Table"
    ));
  }

  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
      props.put(vals[i], vals[i + 1]);
    return props;
  }
}
