package com.browserup.bup.mitmproxy.addons;

public class ProxyManagerAddOn extends AbstractAddon {
  private static final String PROXY_MANAGER_ADDON_FILE = "proxy_manager.py";

  @Override
  public String[] getCommandParams() {
    return new String[]{
            "-s", getAddOnFilePath()
    };
  }

  @Override
  public String getAddOnFileName() {
    return PROXY_MANAGER_ADDON_FILE;
  }
}
