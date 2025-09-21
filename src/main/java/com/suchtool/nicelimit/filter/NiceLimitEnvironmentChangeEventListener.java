package com.suchtool.nicelimit.filter;

import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;

public class NiceLimitEnvironmentChangeEventListener implements ApplicationListener<EnvironmentChangeEvent> {
    private final NiceLimitHandler niceLimitHandler;

    public NiceLimitEnvironmentChangeEventListener(NiceLimitHandler niceLimitHandler) {
        this.niceLimitHandler = niceLimitHandler;
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        niceLimitHandler.doCheckAndUpdateConfig();
    }
}
