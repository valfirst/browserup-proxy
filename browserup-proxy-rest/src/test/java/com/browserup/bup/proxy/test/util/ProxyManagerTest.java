package com.browserup.bup.proxy.test.util;

import com.browserup.bup.MitmProxyServer;
import com.browserup.bup.proxy.MitmProxyManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.browserup.bup.proxy.guice.ConfigModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class ProxyManagerTest {
    protected MitmProxyManager proxyManager;

    public String[] getArgs() {
        return new String[] {};
    }

    @BeforeEach
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new ConfigModule(getArgs()));
        proxyManager = injector.getInstance(MitmProxyManager.class);
    }

    @AfterEach
    public void tearDown() throws Exception {
        for(MitmProxyServer p : proxyManager.get()){
            try{
                proxyManager.delete(p.getPort());
            }catch(Exception e){ }
        }
    }

}
