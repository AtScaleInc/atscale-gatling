package com.customer.executors;

import com.atscale.java.executors.QueryExtractExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MyQueryExtractExecutor extends QueryExtractExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyQueryExtractExecutor.class);

    public static void main(String[] args) {
        MyQueryExtractExecutor executor = new MyQueryExtractExecutor();
        executor.execute();
    }
}
