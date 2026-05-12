package com.yangky.scbotdemo.bwem.build;

import com.yangky.scbotdemo.bwem.region.RegionType;

import java.util.Arrays;
import java.util.List;

/**
 * RegionStrategy
 *
 * @author yangky
 * @Date 2026/5/12 11:42
 */
public class RegionStrategy {
    private List<RegionType> regions;

    public static RegionStrategy forTypes(RegionType... types) {
        RegionStrategy strategy = new RegionStrategy();
        strategy.regions = Arrays.asList(types);
        return strategy;
    }

    public List<RegionType> getRegions() {
        return regions;
    }
}

