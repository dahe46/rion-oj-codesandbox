package com.rionoj.codesandbox.containerpool;

import com.rionoj.codesandbox.model.JdkContainer;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class MyGenericObjectPool {

    private final GenericObjectPool<JdkContainer> pool;
    public MyGenericObjectPool(PooledObjectFactory<JdkContainer> factory) {
        pool = new GenericObjectPool<>(factory);
    }

    public MyGenericObjectPool(PooledObjectFactory<JdkContainer> factory, GenericObjectPoolConfig<JdkContainer> config) {
        this.pool = new GenericObjectPool<>(factory, config);
    }

    public MyGenericObjectPool(PooledObjectFactory<JdkContainer> factory, GenericObjectPoolConfig<JdkContainer> config,
        AbandonedConfig abandonedConfig) {
        this.pool = new GenericObjectPool<>(factory, config,abandonedConfig);
    }

    public JdkContainer borrow() {
        try {
            return this.pool.borrowObject(3000);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void returnObject(JdkContainer jdkContainer) {
        this.pool.returnObject(jdkContainer);
    }
}
