package com.rionoj.codesandbox.controller;

import com.rionoj.codesandbox.JavaDockerCodeSandbox;
import com.rionoj.codesandbox.JavaNativeCodeSandbox;
import com.rionoj.codesandbox.containerpool.MyGenericObjectPool;
import com.rionoj.codesandbox.model.ExecuteCodeReps;
import com.rionoj.codesandbox.model.ExecuteCodeReq;
import com.rionoj.codesandbox.model.JdkContainer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Rion
 * @date: 2023/9/27
 */

@RestController
@RequestMapping("/")
public class CodeSandboxController {

    private static final String AUTH_REQUEST_HEADER = "auth";
    private static final String AUTH_REQUEST_SECRET = "rion";

    @Resource
    private MyGenericObjectPool genericObjectPool;
    @Resource
    private JavaNativeCodeSandbox javaNativeCodeSandbox;
    @Resource
    private JavaDockerCodeSandbox javaDockerCodeSandbox;
    /**
     * 执行代码
     *
     * @param executeCodeReq 执行代码请求信息
     * @return {@link ExecuteCodeReps}
     */
    @PostMapping("/executeCode")
    ExecuteCodeReps executeCode(@RequestBody ExecuteCodeReq executeCodeReq, HttpServletRequest request,
                                HttpServletResponse response) {
        String header = request.getHeader(AUTH_REQUEST_HEADER);
        if (!AUTH_REQUEST_SECRET.equals(header)) {
            response.setStatus(403);
            return null;
        }
        if (executeCodeReq == null) {
            throw new RuntimeException("请求为空");
        }
        JdkContainer jdkContainer = genericObjectPool.borrow();
        ExecuteCodeReps executeCodeReps = javaDockerCodeSandbox.executeCode(executeCodeReq, jdkContainer.getId());
        genericObjectPool.returnObject(jdkContainer);
        System.out.println(executeCodeReq);
        return executeCodeReps;
    }
}
