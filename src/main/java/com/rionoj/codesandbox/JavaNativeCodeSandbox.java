package com.rionoj.codesandbox;

import com.rionoj.codesandbox.model.ExecuteCodeReps;
import com.rionoj.codesandbox.model.ExecuteCodeReq;
import org.springframework.stereotype.Component;

/**
 * Java 原生代码沙箱实现（直接复用模板方法）
 *
 * @author Rion
 * @date: 2023/9/27
 */

@Component
public class JavaNativeCodeSandbox extends JavaCodeSandboxTemplate {

    @Override
    public ExecuteCodeReps executeCode(ExecuteCodeReq executeCodeReq) {
        return super.executeCode(executeCodeReq);
    }
}

