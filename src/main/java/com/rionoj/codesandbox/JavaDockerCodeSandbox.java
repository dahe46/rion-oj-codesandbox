package com.rionoj.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.rionoj.codesandbox.constant.UserCodeConstant;
import com.rionoj.codesandbox.model.ExecuteCodeReps;
import com.rionoj.codesandbox.model.ExecuteCodeReq;
import com.rionoj.codesandbox.model.ExecuteMessage;
import com.rionoj.codesandbox.model.JudgeInfo;
import com.rionoj.codesandbox.utils.DockerUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandboxTemplate {

    private static final long TIME_OUT = 5000L;
    private static final String USER_CODE_FILE_NAME = "Main.java";
    private final DockerClient dockerClient = DockerUtils.getClient();

    @Override
    public ExecuteCodeReps executeCode(ExecuteCodeReq executeCodeReq) {
        return super.executeCode(executeCodeReq);
    }

    public ExecuteCodeReps executeCode(ExecuteCodeReq executeCodeReq, String containerId) {
        return super.executeCode(executeCodeReq, containerId);
    }

    @Override
    public File saveCode(String code) {
        // 1、把用户代码保存为文件
        return super.saveCode(code);
    }

    public File saveCode(String code, String containerId) {
        // 1、把用户代码保存为文件
        String userDir = System.getProperty("user.dir");
        String tmpCodePath = userDir + File.separator + UserCodeConstant.TMP_CODE_DIR_NAME;
        if (!FileUtil.exist(tmpCodePath)) {
            FileUtil.mkdir(tmpCodePath);
        }
        InspectContainerResponse exec = dockerClient.inspectContainerCmd(containerId).exec();
        String containerName = exec.getName();
        // 把用户代码隔离存放
        String userCodeParentPath = tmpCodePath + File.separator + containerName;
        String userCodePath = userCodeParentPath + File.separator + USER_CODE_FILE_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    @Override
    public boolean delFile(File userCodeFile, String containerId) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.clean(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    public boolean delFile(File userCodeFile) {
        return super.delFile(userCodeFile);
    }

    /**
     * 3. 创建容器，把文件复制到容器内
     *
     * @param userCodeFile 用户代码文件
     * @param inputList    输入用例
     * @return
     */


    @Override
    public List<ExecuteMessage> runCode(File userCodeFile, List<String> inputList, String containerId) {
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
            ExecuteMessage executeMessage = new ExecuteMessage();
            final String[] message = {null};
            final String[] errorMessage = {null};
            long time = 0L;
            // 判断是否超时
            final boolean[] timeout = {true};
            final long[] timeMillis = {0L};
            String execId = execCreateCmdResponse.getId();
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onStart(Closeable stream) {
                    super.onStart(stream);
                }

                @Override
                public void onComplete() {
                    // 如果执行完成，则表示没超时
                    timeout[0] = false;
                    System.out.println("按时完成");
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    String payLoad = new String(frame.getPayload());
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = payLoad;
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        // todo
                        // 结果处理，去掉换行符
                        String result = payLoad.replace("\n", "");
                        if (!StrUtil.isEmpty(result)) {
                            message[0] = result;
                            System.out.println("输出结果：" + result);
                        }
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};

            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            CompletableFuture<Statistics> future = new CompletableFuture<>();
            ResultCallback<Statistics> statisticsResultCallback = new ResultCallback<Statistics>() {

                    @Override
                    public void onNext(Statistics statistics) {
                        Long usage = statistics.getMemoryStats().getUsage();
                        maxMemory[0] = Math.max(usage, maxMemory[0]);
                        future.complete(statistics);
                    }

                    @Override
                    public void close() throws IOException {

                    }

                    @Override
                    public void onStart(Closeable closeable) {

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                };
            statsCmd.exec(statisticsResultCallback);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
                future.get();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
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
