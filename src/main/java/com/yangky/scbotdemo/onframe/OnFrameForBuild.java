package com.yangky.scbotdemo.onframe;

import com.yangky.scbotdemo.bwem.build.BuildExecutor;
import com.yangky.scbotdemo.bwem.build.BuildingDemandManager;
import com.yangky.scbotdemo.util.Times;
import org.springframework.stereotype.Component;

/**
 * OnFrameBuild
 *
 * @author yangky
 * @Date 2026/4/28 17:03
 */
@Component
public class OnFrameForBuild extends OnFrame {

    @Override
    public Integer getInterval() {
        return Times.BUILD_INTERVAL;
    }

    @Override
    public void onFrame(Integer frame) {
        BuildingDemandManager.syncToTasks();
        BuildExecutor.execute();
    }
}
