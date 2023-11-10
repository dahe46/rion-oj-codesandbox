package com.rionoj.codesandbox.containerpool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.rionoj.codesandbox.constant.DockerConstant;
import com.rionoj.codesandbox.constant.UserCodeConstant;
import com.rionoj.codesandbox.model.JdkContainer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class JdkContainerFactory {

    private static final List<String> ids = new ArrayList<>();
    public static JdkContainer createContainer(DockerClient dockerClient, String image) {
        List<Container> containerList = dockerClient.listContainersCmd().withShowAll(true).exec();
        List<Container> localContainer = containerList.stream().filter(container -> container.getImage().equals(image))
                .collect(Collectors.toList());
        final JdkContainer[] jdkContainer = new JdkContainer[1];
        if (localContainer.size() > 0) {
            // 在localContainer对象上添加锁，确保多线程安全
            synchronized (localContainer) {
                for (Container container : localContainer) {
                    if (!ids.contains(container.getId())) {
                        ids.add(container.getId());
                        // todo 检测容器是否正常运行
                        // 容器未启动则启动容器
                        if (!container.getStatus().startsWith("Up")) {
                            // 启动容器
                            dockerClient.startContainerCmd(container.getId()).exec();
                        }
                        HostConfig hostConfig = dockerClient.inspectContainerCmd(container.getId()).exec().getHostConfig();
                        jdkContainer[0] = JdkContainer.builder()
                                .id(container.getId())
                                .memory(hostConfig.getMemory())
                                .cpuCount(hostConfig.getCpuCount())
                                .build();
                        break;
                    }
                }
            }
        }
        if (jdkContainer[0] != null) {
            return jdkContainer[0];
        }

        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        String containerName = DockerConstant.CONTAINER_NAME + RandomUtil.randomString(5);
        // 指定容器名
        containerCmd.withName(containerName);
        // 创建容器挂载路径（用户提交代码存放路径）
        String userDir = System.getProperty("user.dir");
        String tmpCodePath = userDir + File.separator + UserCodeConstant.TMP_CODE_DIR_NAME;
        String userCodeParentPath = tmpCodePath + File.separator + containerName;
        FileUtil.mkdir(userCodeParentPath);
        // 配置容器
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(50 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        // 启动容器
        dockerClient.startContainerCmd(createContainerResponse.getId()).exec();
        jdkContainer[0] = JdkContainer.builder()
                .id(createContainerResponse.getId())
                .memory(hostConfig.getMemory())
                .cpuCount(hostConfig.getCpuCount())
                .build();
        return jdkContainer[0];
    }
}
