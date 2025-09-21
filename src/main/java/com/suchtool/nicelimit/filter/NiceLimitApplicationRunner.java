package com.suchtool.nicelimit.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@Slf4j
public class NiceLimitApplicationRunner implements ApplicationRunner {

    private final NiceLimitHandler niceLimitHandler;

    public NiceLimitApplicationRunner(NiceLimitHandler niceLimitHandler) {
        this.niceLimitHandler = niceLimitHandler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            niceLimitHandler.doCheckAndUpdateConfig();
        } catch (Exception e) {
            log.error("nicelimit application runner error", e);
        }

    }
}
