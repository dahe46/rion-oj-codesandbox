package com.rionoj.codesandbox;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.PullResponseItem;
import com.rionoj.codesandbox.constant.DockerConstant;
import com.rionoj.codesandbox.utils.DockerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@Slf4j
@SpringBootApplication
public class MainApplication {

    @PostConstruct
    public void initImage() {
        DockerClient dockerClient = DockerUtils.getClient();
        String image = DockerConstant.JDK_IMAGE;
        List<String> imagesRepoTags = DockerUtils.getImagesRepoTags(dockerClient);
        boolean hasImage = imagesRepoTags.contains(image);
        if (!hasImage) {
            log.info("未检测到镜像：{}", image);
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    log.info("拉取镜像中：{}", image);
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                log.error("拉取镜像异常：{}", image);
                throw new RuntimeException(e);
            }
            log.info("拉取镜像完成：{}", image);
        } else {
            log.info("检测到镜像：{}", image);
        }
        try {
            dockerClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

}
