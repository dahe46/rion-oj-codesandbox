package com.rionoj.codesandbox.model;

import lombok.Data;

/**
 * 裁判信息
 *
 * @author Rion
 * @date 2023/9/6
 */
@Data
public class JudgeInfo {

    /**
     *  程序执行信息
     */
    private String message;
    /**
     *  消耗内存
     */
    private Long memory;
    /**
     *  执行时间
     */
    private Long time;

}
