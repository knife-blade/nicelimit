package com.suchtool.nicelimit.listener;

import com.suchtool.nicelimit.handler.NiceLimitHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.CollectionUtils;

import java.util.Set;

@Slf4j
public class NiceLimitEnvironmentChangeEventListener implements ApplicationListener<EnvironmentChangeEvent> {
    private final NiceLimitHandler niceLimitHandler;

    public NiceLimitEnvironmentChangeEventListener(NiceLimitHandler niceLimitHandler) {
        this.niceLimitHandler = niceLimitHandler;
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        try {
            Set<String> keys = event.getKeys();
            if (!CollectionUtils.isEmpty(keys)) {
                boolean requireUpdateConfig = false;
                for (String key : keys) {
                    if (key.startsWith("suchtool.nicelimit")) {
                        requireUpdateConfig = true;
                        break;
                    }
                }

                if (requireUpdateConfig) {
                    niceLimitHandler.doCheckAndUpdateConfig();
                }
            }
        } catch (Exception e) {
            log.error("nicelimit EnvironmentChangeEventListener error", e);
        }
    }
}
