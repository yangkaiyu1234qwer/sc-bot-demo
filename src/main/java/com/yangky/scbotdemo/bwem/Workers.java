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

    private static final Set<Unit> experiencedBuilders = new HashSet<>();

    // 建筑完成时标记
    public static void markAsExperiencedBuilder(Unit worker) {
        if (worker != null) {
            experiencedBuilders.add(worker);
        }
    }

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

    public static Unit getBuilderWorker(Player player, Position target) {
        Unit bestBuilder = null;
        int bestDist = Integer.MAX_VALUE;
        // 第 1 步：从有经验的 SCV 中找最近的、可达的
        for (Unit scv : experiencedBuilders) {
            if (scv == null || !scv.exists() || !scv.isIdle()) {
                continue;
            }
            // 检查可达性（距离基地 <= 20 格）
            if (!isReachable(scv)) {
                continue;
            }
            int dist = scv.getTilePosition().getApproxDistance(target.toTilePosition());
            if (dist < bestDist) {
                bestDist = dist;
                bestBuilder = scv;
            }
        }
        // 如果找到合适的有经验 SCV，返回
        if (bestBuilder != null && bestDist <= 30) {
            return bestBuilder;
        }
        // 第 2 步：降级到普通工人
        return getAWorker(player, target);
    }

    private static boolean isReachable(Unit scv) {
        Unit mainBase = Bases.getMainBaseUnit();
        if (mainBase == null) return true;
        int distToBase = scv.getTilePosition().getApproxDistance(mainBase.getTilePosition());
        return distToBase <= 20;  // 20 格以内认为可达
    }

    /**
     * 让 SCV 回到基地附近（5-10 格范围内）
     */
    public static void returnToBaseArea(Unit worker) {
        if (worker == null || !worker.exists()) {
            return;
        }

        Unit mainBase = Bases.getMainBaseUnit();
        if (mainBase == null) {
            return;
        }

        int distToBase = worker.getTilePosition().getApproxDistance(mainBase.getTilePosition());

        // 如果已经在 5-10 格范围内，不需要移动
        if (distToBase >= 5 && distToBase <= 10) {
            return;
        }

        // 计算朝向基地的方向点（在基地 8 格距离）
        Position target = calculateDirectionTowardsBase(worker, mainBase);
        worker.rightClick(target);
    }

    /**
     * 计算朝向基地的方向点
     */
    private static Position calculateDirectionTowardsBase(Unit worker, Unit base) {
        Position workerPos = worker.getPosition();
        Position basePos = base.getPosition();

        // 计算从工人到基地的向量
        double dx = basePos.getX() - workerPos.getX();
        double dy = basePos.getY() - workerPos.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 避免除以 0
        if (distance == 0) {
            return basePos;
        }

        // 归一化后，缩放到 8 格（256 像素）的距离
        int targetDist = 8 * 32;  // 8 格 = 256 像素
        int targetX = workerPos.getX() + (int) (dx / distance * targetDist);
        int targetY = workerPos.getY() + (int) (dy / distance * targetDist);

        return new Position(targetX, targetY);
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
                .filter(e -> !e.isGatheringGas() || e.isCarryingGas())
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
            // ✅ 跳过有建造经验的 SCV（让它们专门负责建造/维修）
            if (experiencedBuilders.contains(e)) {
                return;
            }
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

    // 维修时优先用有经验的 SCV
    public static Unit getRepairWorker(Player player, Unit damagedBuilding) {
        // 优先从有经验的 SCV 中找
        for (Unit scv : experiencedBuilders) {
            if (scv != null && scv.exists() && scv.isIdle()) {
                return scv;  // 即使被堵在外面，也可以修附近的建筑
            }
        }
        // 没有合适的，用普通工人
        return getBuilderWorker(player, damagedBuilding.getPosition());
    }

    /**
     * 清理工人分配缓存
     */
    public static void clearCache() {
        mineralWorkerCache.invalidateAll();
        experiencedBuilders.clear();
    }

}
