package com.rionoj.codesandbox;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.rionoj.codesandbox.model.ExecuteCodeReps;
import com.rionoj.codesandbox.model.ExecuteCodeReq;
import com.rionoj.codesandbox.model.ExecuteMessage;
import com.rionoj.codesandbox.model.JudgeInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    public static final String CONTAINER_NAME = "rion-openjdk";
    private static final long TIME_OUT = 5000L;

    private static final Boolean FIRST_INIT = false;

    @Override
    public ExecuteCodeReps executeCode(ExecuteCodeReq executeCodeReq) {
        return super.executeCode(executeCodeReq);
    }

    /**
     * 3. 创建容器，把文件复制到容器内
     * @param userCodeFile 用户代码文件
     * @param inputList 输入用例
     * @return
     */
    @Override
    public List<ExecuteMessage> runCode(File userCodeFile, List<String> inputList) {
        // 获取默认的 Docker Client
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 拉取镜像
        String image = "openjdk:8-alpine";
        if (FIRST_INIT) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载完成");
        }
        // 查找指定容器名对应的容器 ID，复用容器
        String containerId = dockerClient.listContainersCmd().withNameFilter(Collections.singleton(CONTAINER_NAME))
                .exec()
                .stream()
                .findFirst()
                .map(Container::getId)
                .orElse(null);
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        if (containerId != null) {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
        }
//        else {
//
//
//            // 获取容器的配置信息
////            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
////            // 获取容器的 Mounts 配置
////            List<InspectContainerResponse.Mount> mounts = container.getMounts();
////            // 遍历 Mounts 配置，找到要修改的 Mount
////            for (InspectContainerResponse.Mount mount : mounts) {
////                if ("/app".equals(mount.getDestination())) {
////                    // 修改 Mount 的 Source 属性
////                    System.out.println(mount.getSource());
////                    mount.withSource(userCodeParentPath); // 将 "/new/source/path" 替换为新的源路径
////                }
////            }
////            for (Mount mount : mounts) {
////                if ("/app".equals(mount.getDestination())) {
////                    // 修改 Mount 的 Source 属性
////                    mount.withSource("/new/source/path"); // 将 "/new/source/path" 替换为新的源路径
////                }
////            }
//
//            // 更新容器的配置
////            dockerClient.updateContainerCmd(containerId)
////                    .withMounts(mounts) // 设置修改后的 Mounts 配置
////                    .exec();
//        }
        System.out.println("新建容器");
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(image);
        // 指定容器名
        containerCmd.withName(CONTAINER_NAME);
        // 容器配置
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
//        hostConfig.withSecurityOpts(Arrays.asList("seccomp=安全管理配置字符串"));
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
        System.out.println(createContainerResponse);
        containerId = createContainerResponse.getId();
        // 启动容器
        dockerClient.startContainerCmd(containerId).exec();
        // todo 检测容器是否正常运行

        // docker exec keen_blackwell java -cp /app Main 1 3
        // 执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);

            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            // 判断是否超时
            final boolean[] timeout = {true};
            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    // 如果执行完成，则表示没超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {

                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() throws IOException {

                }

                @Override
                public void onStart(Closeable closeable) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {
                    System.out.println("完成");
                }
            });
            statsCmd.exec(statisticsResultCallback);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(5000L);
                executeMessage.setMessage(message[0]);
                executeMessage.setErrorMessage(errorMessage[0]);
                executeMessage.setTime(time);
                executeMessage.setMemory(maxMemory[0]);
                executeMessageList.add(executeMessage);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        executeMessageList.forEach(System.out::println);
        return executeMessageList;
    }

    @Override
    public ExecuteCodeReps getOutput(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeReps executeCodeReps = new ExecuteCodeReps();
        List<String> outputList = new ArrayList<>();
        // 取用时最大值，便于判断是否超时
        long maxTime = 0;
        long maxMemory = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeReps.setMessage(errorMessage);
                // 用户提交的代码执行中存在错误
                executeCodeReps.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            Long memory = executeMessage.getMemory();
            System.out.println("返回内存：" + memory);
            System.out.println(executeMessage);
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
            if (memory != null) {
                maxMemory = Math.max(maxMemory, memory);
            }
        }
        // 正常运行完成
        if (outputList.size() == executeMessageList.size()) {
            executeCodeReps.setStatus(1);
        }
        executeCodeReps.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 设置最大运行内存（from docker）
        judgeInfo.setMemory(maxMemory);
        System.out.println("最大内存消耗：" + maxMemory);
        executeCodeReps.setJudgeInfo(judgeInfo);
        return executeCodeReps;
    }

}
