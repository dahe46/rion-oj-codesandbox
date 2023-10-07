package com.rionoj.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 执行结果返回
 *
 * @author Rion
 * @date 2023/9/26
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCodeReps {

    /**
     * 输出列表
     */
    private List<String> outputList;

    /**
     * 消息
     */
    private String message;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 裁判信息
     */
    private JudgeInfo judgeInfo;
}
