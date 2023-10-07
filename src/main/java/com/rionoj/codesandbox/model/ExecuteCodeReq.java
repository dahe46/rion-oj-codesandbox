package com.rionoj.codesandbox.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Rion
 * @date: 2023/9/26
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeReq {

    private List<String> inputList;

    private String code;

    private String language;
}
