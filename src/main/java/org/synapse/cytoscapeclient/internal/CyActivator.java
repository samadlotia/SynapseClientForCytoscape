package org.synapse.cytoscapeclient.internal;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.ServiceProperties;

public class CyActivator extends AbstractCyActivator {
  public void start(BundleContext bc) {
    System.out.println((new java.util.Date()).toString() + " started: " + getClass().getName());
    
    final CyApplicationConfiguration cyAppConf = getService(bc, CyApplicationConfiguration.class);

    final APIKeyMgr apiKeyMgr = new APIKeyMgr(cyAppConf.getAppConfigurationDirectoryLocation(this.getClass()));

    registerService(bc, new APIKeyTaskFactory(apiKeyMgr), TaskFactory.class, ezProps(
      ServiceProperties.TITLE, "API Key...",
      ServiceProperties.PREFERRED_MENU, "Apps.Synapse"
    ));
  }

  private static Properties ezProps(String... vals) {
    final Properties props = new Properties();
    for (int i = 0; i < vals.length; i += 2)
      props.put(vals[i], vals[i + 1]);
    return props;
  }
}
