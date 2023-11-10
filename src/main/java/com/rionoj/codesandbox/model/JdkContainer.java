package com.rionoj.codesandbox.model;

import lombok.Builder;
import lombok.Data;

import java.io.Closeable;
import java.io.IOException;

@Data
@Builder
public class JdkContainer implements Closeable {

    /**
     *
     */
    private String id;
    /**
     *
     */
    private Long memory;
    /**
     *
     */
    private Long cpuCount;

    @Override
    public void close() throws IOException {
    }
}
