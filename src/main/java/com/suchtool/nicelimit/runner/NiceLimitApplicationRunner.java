package com.suchtool.nicelimit.runner;

import com.suchtool.nicelimit.handler.NiceLimitHandler;
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
    public void run(ApplicationArguments args) {
        try {
            niceLimitHandler.doCheckAndUpdateConfig();
        } catch (Exception e) {
            log.error("nicelimit application runner error", e);
        }
    }
}
