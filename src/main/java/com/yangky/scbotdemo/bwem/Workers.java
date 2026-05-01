package com.yangky.scbotdemo.bwem;

import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwem.Base;
import bwem.Mineral;
import bwem.Neutral;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * CustomWorker
 *
 * @author yangky
 * @Date 2026/4/27 21:23
 */
public class Workers {
    // 计时缓存来记录水晶与对应工人的关联关系，以优化后续分配
    private static final Cache<Unit, Set<Unit>> mineralWorkerCache = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();


    public static int getWeightToChooseForBuild(Position buildPosition, Unit worker) {
        int weight = 0;

        // ✅ 第一优先级：是否携带资源（不带矿的优先）
        if (worker.isCarryingMinerals() || worker.isCarryingGas()) {
            weight += 10000;  // 带资源的工人权重大幅增加（降序时排后面）
        }

        // ✅ 第二优先级：是否空闲
        if (!worker.isIdle()) {
            weight += 1000;   // 忙碌的工人权重增加
        }

        // ✅ 第三优先级：距离
        int distance = worker.getDistance(buildPosition);
        weight += distance;   // 距离越远，权重越大

        return weight;
    }

    /**
     * 获取基地周围在采集水晶的农民
     */
    public static List<Unit> getWorkersByMineral(Player player, Unit base) {
        return base.getUnitsInRadius(12, (e) -> e.getType().isWorker() && e.isGatheringMinerals() && e.getPlayer().equals(player));
    }

    /**
     * 获取一个工人用来建造或者干别的
     */
    public static Unit getAWorker(Player player, Position position) {
        //优先选择真正空闲的工人（不在采矿、不在建造）
        List<Unit> workers = player
                .getUnits()
                .stream()
                .filter(e -> e.getType().isWorker() && !e.isConstructing())
                .filter(e -> !Builds.getBuildingWorkers().contains(e))  //排除建造任务中的工人
                .sorted(Comparator.comparingInt(o -> Workers.getWeightToChooseForBuild(position, o)))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(workers)) {
            workers = player
                    .getUnits()
                    .stream()
                    .filter(e -> e.getType().isWorker())
                    .filter(e -> !Builds.getBuildingWorkers().contains(e))  //排除建造任务中的工人
                    .sorted(Comparator.comparingInt(o -> Workers.getWeightToChooseForBuild(position, o)))
                    .collect(Collectors.toList());
        }
        return CollectionUtils.isEmpty(workers) ? null : workers.get(0);
    }

    /**
     * 指挥工人在指定分矿就近采矿，最近的分矿没有，就找最近的水晶，如果没有水晶，就采集气矿
     */
    public static void goGatherLessLoader(Unit worker, Unit base) {
        Unit mineral = getClosestMineralsLessWorkers(worker, base);
        if (mineral != null) {
            worker.gather(mineral);
            System.out.println("分配农民:" + worker + "去采集:" + mineral.getTilePosition() + " " + mineral.getType());
            Set<Unit> gatheringWorkers = getGatheringWorkersByMineral(mineral);
            gatheringWorkers.add(worker);
            mineralWorkerCache.put(mineral, gatheringWorkers);
        }
    }

    /**
     * 结合已分配记录给工人分配最优水晶
     */
    public static void goGatherLessLoader(List<Unit> workers, Unit base) {
        workers.forEach(e -> {
            goGatherLessLoader(e, base);
        });
    }

    private static Set<Unit> getGatheringWorkersByMineral(Unit mineral) {
        Set<Unit> gatheringWorkers = mineralWorkerCache.getIfPresent(mineral);
        if (!CollectionUtils.isEmpty(gatheringWorkers)) {
            // 过滤掉不能采集的工人
            gatheringWorkers = gatheringWorkers
                    .stream()
                    .filter(Unit::canGather)
                    .collect(Collectors.toSet());
        } else {
            gatheringWorkers = new HashSet<>();
        }
        return gatheringWorkers;
    }

    public static Base getBaseFormBaseUnit(Unit baseUnit) {
        return Games.bwem.getMap().getBases().stream()
                .min(Comparator.comparingInt(e -> baseUnit.getDistance(e.getLocation().toPosition())))
                .orElse(null);
    }

    public static Unit getClosestMineralsLessWorkers(Unit worker, Unit baseUnit) {
        Unit mineral = null;
        if (baseUnit != null) {
            Base base = getBaseFormBaseUnit(baseUnit);
            if (base != null) {
                List<Unit> mineralList = base.getMinerals()
                        .stream()
                        .map(Neutral::getUnit)
                        .sorted(Comparator.comparingDouble(e -> getMineralSortWeight(e, base.getCenter())))// 先按采集农民数排序
                        .collect(Collectors.toList());
                mineral = mineralList.stream().min(Comparator.comparingDouble(e -> getMineralSortWeight(e, base.getCenter()))).orElse(null);
                if (mineral != null) {
                    Set<Unit> gatheringWorkers = getGatheringWorkersByMineral(mineral);
                    gatheringWorkers.add(worker);
                    mineralWorkerCache.put(mineral, gatheringWorkers);
                }
            }
            if (mineral == null) {
                mineral = getClosestMinerals(worker, base);
            }
        }
        return mineral;
    }

    public static Unit getClosestMinerals(Unit worker, Base base) {
        Unit mineralUnit = null;
        if (base != null) {
            Mineral mineral = base.getMinerals()
                    .stream()
                    .min(Comparator.comparingInt(m -> worker.getDistance(m.getUnit().getPosition())))
                    .orElse(null);
            if (mineral != null) {
                mineralUnit = mineral.getUnit();
            }
        }
        if (mineralUnit == null) {
            mineralUnit = Games.game.getMinerals().stream()
                    .min(Comparator.comparingInt(worker::getDistance)).orElse(null);
        }
        if (mineralUnit == null) {
            mineralUnit = Games.game.getGeysers().stream()
                    .filter(e -> e.isCompleted() && e.getPlayer() == worker.getPlayer())
                    .min(Comparator.comparingInt(worker::getDistance)).orElse(null);
        }
        return mineralUnit;
    }

    public static double getMineralSortWeight(Unit mineral, Position position) {
        return Double.parseDouble(getGatheringWorkersByMineral(mineral).size() + "." + mineral.getDistance(position));
    }

    /**
     * 清理工人分配缓存
     */
    public static void clearCache() {
        mineralWorkerCache.invalidateAll();
    }

}
