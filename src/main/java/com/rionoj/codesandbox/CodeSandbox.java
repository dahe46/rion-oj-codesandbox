package com.rionoj.codesandbox;

import com.rionoj.codesandbox.model.ExecuteCodeReps;
import com.rionoj.codesandbox.model.ExecuteCodeReq;

/**
 * 代码沙箱接口
 *
 * @author Rion
 * @date 2023/09/26
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeReq 执行代码请求信息
     * @return {@link ExecuteCodeReps}
     */
    ExecuteCodeReps executeCode(ExecuteCodeReq executeCodeReq);
}
