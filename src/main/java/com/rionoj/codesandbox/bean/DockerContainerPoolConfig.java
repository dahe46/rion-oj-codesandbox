package com.rionoj.codesandbox.bean;

import com.rionoj.codesandbox.containerpool.DockerContainerFactory;
import com.rionoj.codesandbox.containerpool.MyGenericObjectPool;
import com.rionoj.codesandbox.model.JdkContainer;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * docker 容器池配置
 *
 * @author Rion
 * @date 2023/9/28
 */
@Configuration
public class DockerContainerPoolConfig {

    @Bean
    public MyGenericObjectPool genericObjectPool() {
        GenericObjectPoolConfig<JdkContainer> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(2);
        config.setMaxIdle(1);
        return new MyGenericObjectPool(new DockerContainerFactory(), config);
    }

}
