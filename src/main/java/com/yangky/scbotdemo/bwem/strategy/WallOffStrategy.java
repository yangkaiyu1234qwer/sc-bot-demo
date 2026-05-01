package com.yangky.scbotdemo.bwem.strategy;

import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.walloff.WallOffExecutor;
import com.yangky.scbotdemo.util.Properties;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * WallOffStrategy
 *
 * @author yangky
 * @Date 2026/4/29 18:14
 */
@Component
@AllArgsConstructor
public class WallOffStrategy extends AbstractStrategy {
    private Properties properties;

    @Override
    public boolean apply() {
        return properties.isWallOffOn();
    }

    @Override
    public void execute(Integer frame) {
        if (Units.getTeamUnits(2).size() > 0) {
            WallOffExecutor.execute(Units.getTeamUnits(2).iterator().next());
        }
    }
}
