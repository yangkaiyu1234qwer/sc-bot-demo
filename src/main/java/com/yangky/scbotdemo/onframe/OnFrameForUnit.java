package com.yangky.scbotdemo.onframe;

import bwapi.Unit;
import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.Workers;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * OnFrameForUnit - 定期清理工人状态映射
 *
 * @author yangky
 * @Date 2026/5/1 19:57
 */
@Component
public class OnFrameForUnit extends OnFrame {
    @Override
    public Integer getInterval() {
        return 10;  // 每 10 帧清理一次
    }

    @Override
    public void onFrame(Integer frame) {
        Set<Unit> allSCVs = Units.getSelfUnits(UnitType.Terran_SCV);
        if (allSCVs.isEmpty()) {
            return;
        }
        // ✅ 统一清理不在采集状态的工人
        Workers.cleanupInactiveWorkers(allSCVs);
    }
}
