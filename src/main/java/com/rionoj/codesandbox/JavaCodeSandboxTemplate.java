package com.rionoj.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.rionoj.codesandbox.model.ExecuteCodeReps;
import com.rionoj.codesandbox.model.ExecuteCodeReq;
import com.rionoj.codesandbox.model.ExecuteMessage;
import com.rionoj.codesandbox.model.JudgeInfo;
import com.rionoj.codesandbox.utils.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {

    private static final String TMP_CODE_DIR_NAME = "tmpCode";
    private static final String USER_CODE_FILE_NAME = "Main.java";
    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeReps executeCode(ExecuteCodeReq executeCodeReq) {
        List<String> inputList = executeCodeReq.getInputList();
        String code = executeCodeReq.getCode();
        String language = executeCodeReq.getLanguage();

        // 1、将用户代码保存为文件
        File userCodeFile = saveCode(code);

        // 2、编译代码
        ExecuteMessage compileCodeExecuteMessage = compileCode(userCodeFile);
        System.out.println(compileCodeExecuteMessage);

        // 3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runCode(userCodeFile, inputList);

        // 4. 收集整理输出结果
        ExecuteCodeReps executeCodeReps = getOutput(executeMessageList);

        // 5. 文件清理
        boolean delFile = delFile(userCodeFile);
        if (!delFile) {
            log.warn("delete file error，userCodePath：{}", userCodeFile.getAbsolutePath());
        }

        return executeCodeReps;
    }

    /**
     * 1、将用户代码保存为文件
     * @param code 用户代码文本
     * @return File
     */
    public File saveCode(String code) {
        // 1、把用户代码保存为文件
        String userDir = System.getProperty("user.dir");
        String tmpCodePath = userDir + File.separator + TMP_CODE_DIR_NAME;
        if (!FileUtil.exist(tmpCodePath)) {
            FileUtil.mkdir(tmpCodePath);
        }
        // 把用户代码隔离存放
        String userCodeParentPath = tmpCodePath + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + USER_CODE_FILE_NAME;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    /**
     * 2、编译代码
     * @param userCodeFile 用户代码文件
     * @return
     */
    public ExecuteMessage compileCode(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            Process process = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(process, "编译");
            if (executeMessage.getExitValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (IOException e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. 执行代码，得到输出结果
     * @param userCodeFile 用户代码文件
     * @param inputList 输入用例
     * @return
     */
    public List<ExecuteMessage> runCode(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
//            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=%s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
//                return getErrorResponse(e);
                throw new RuntimeException(e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 收集整理输出结果
     * @param executeMessageList 执行结果
     * @return
     */
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

    /**
     * 5. 文件清理
     * @param userCodeFile 用户代码文件
     * @return
     */
    public boolean delFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del ? "成功" : "失败"));
            return del;
        }
        return true;
    }

    /**
     * 6、获取错误响应
     *
     * @param e
     * @return
     */
    public ExecuteCodeReps getErrorResponse(Throwable e) {
        ExecuteCodeReps executeCodeReps = new ExecuteCodeReps();
        executeCodeReps.setOutputList(new ArrayList<>());
        executeCodeReps.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeReps.setStatus(2);
        executeCodeReps.setJudgeInfo(new JudgeInfo());
        return executeCodeReps;
    }
}
