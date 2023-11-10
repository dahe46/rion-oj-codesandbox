package com.rionoj.codesandbox.containerpool;

import com.github.dockerjava.api.DockerClient;
import com.rionoj.codesandbox.constant.DockerConstant;
import com.rionoj.codesandbox.model.JdkContainer;
import com.rionoj.codesandbox.utils.DockerUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.stereotype.Component;

@Component
public class DockerContainerFactory implements PooledObjectFactory<JdkContainer> {

    private final DockerClient dockerClient = DockerUtils.getClient();

    @Override
    public void activateObject(PooledObject<JdkContainer> pooledObject) throws Exception {

    }

    @Override
    public void destroyObject(PooledObject<JdkContainer> pooledObject) throws Exception {

    }

    @Override
    public PooledObject<JdkContainer> makeObject() throws Exception {
        // 创建容器
        JdkContainer container = JdkContainerFactory.createContainer(dockerClient, DockerConstant.JDK_IMAGE);
        return new DefaultPooledObject<>(container);
    }

    @Override
    public void passivateObject(PooledObject<JdkContainer> pooledObject) throws Exception {

    }

    @Override
    public boolean validateObject(PooledObject<JdkContainer> pooledObject) {
        return false;
    }
}
