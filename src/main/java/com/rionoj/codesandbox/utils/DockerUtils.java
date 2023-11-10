package com.rionoj.codesandbox.utils;

import cn.hutool.core.thread.ThreadUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DockerUtils {

    /**
     * 连接地址
     */
    public static final String URL = "tcp://192.168.182.1:2375";

    /**
     * 获取docker客户端连接
     *
     * @return {@code DockerClient}
     */
    public static DockerClient getClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(URL) // 设置 Docker 守护进程的连接信息
                .build();
        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory()
                .withReadTimeout(5000) // 设置读取超时时间
                .withConnectTimeout(5000); // 设置连接超时时间
        return DockerClientBuilder.getInstance(config)
                .withDockerCmdExecFactory(dockerCmdExecFactory)
                .build();
    }

    /**
     * 获取镜像名列表
     *
     * @param dockerClient docker客户端
     * @return {@code List<String>}
     */
    public static List<String> getImagesRepoTags(DockerClient dockerClient) {
        List<Image> imageList = dockerClient.listImagesCmd().exec();
        return imageList.stream()
                .map(image -> image.getRepoTags()[0])
                .collect(Collectors.toList());
    }

    public static void test() {
        DockerClient client = getClient();
        final long[] memory = {0L};
        CompletableFuture<Statistics> future = new CompletableFuture<>();
        ResultCallback<Statistics> resultCallback = new ResultCallback<Statistics>() {
            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onNext(Statistics statistics) {
                memory[0] = statistics.getMemoryStats().getUsage();
                future.complete(statistics);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {

            }
        };
        client.statsCmd("e79145fea874").exec(resultCallback);
        System.out.println(memory[0]);
        System.out.println("关闭");
        try {
            System.out.println("阻塞等待中...");
            Statistics statistics = future.get();
            System.out.println(statistics.getMemoryStats().getUsage());
            System.out.println(memory[0]);
            System.out.println("关闭");
        }  catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        DockerClient client = getClient();
        List<Image> exec = client.listImagesCmd().withShowAll(true).exec();
        System.out.println(exec);
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }
}
