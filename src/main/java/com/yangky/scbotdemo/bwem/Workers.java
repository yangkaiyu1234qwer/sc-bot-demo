package com.yangky.scbotdemo.bwem;

import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwem.Base;
import bwem.Mineral;
import bwem.Neutral;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工人管理类 - 负责工人分配、状态追踪
 *
 * @author yangky
 * @Date 2026/4/27 21:23
 */
public class Workers {
    // 工人 -> 资源点（1对1映射）
    private static final Map<Unit, Unit> workerToResourceMap = new HashMap<>();

    // 有建造经验的 SCV
    private static final Set<Unit> experiencedBuilders = new HashSet<>();

    // ==================== 标记与查询（只管添加，不管删除）====================
    public static boolean isGatheringWorker(Unit worker) {
        return workerToResourceMap.containsKey(worker);
    }

    public static boolean isGasWorker(Unit worker) {
        return workerToResourceMap.entrySet()
                .stream().anyMatch(e -> e.getValue().getType() == UnitType.Terran_Refinery
                        && e.getKey().equals(worker));
    }


    public static long getGasWorkerCount(Unit refinery) {
        return workerToResourceMap.entrySet().stream().filter(e -> e.getValue().getType() == UnitType.Terran_Refinery && e.getValue() == refinery).count();
    }

    public static long getMineralWorkerCount(Unit mineral) {
        return workerToResourceMap.entrySet().stream().filter(e -> e.getValue().getType() == UnitType.Resource_Mineral_Field && e.getValue() == mineral).count();
    }

    /**
     * 清理所有不在采集状态的工人映射（每帧调用）
     */
    public static void cleanupInactiveWorkers(Set<Unit> allSCVs) {
        Set<Unit> toRemove = new HashSet<>();
        for (Unit worker : allSCVs) {
            // 工人不存在（死亡）
            if (!worker.exists()) {
                toRemove.add(worker);
                continue;
            }
            Unit orderTarget = worker.getOrderTarget();
            if (orderTarget != null && orderTarget.exists()
                    && (orderTarget.getType() == UnitType.Resource_Mineral_Field || orderTarget.getType() == UnitType.Terran_Refinery)) {
                workerToResourceMap.put(worker, orderTarget);
            }
            // 不在采集状态（包括返回基地、空闲、建造等其他任务）
            if (!isGatheringOrder(worker)) {
                toRemove.add(worker);
            }
        }
        // 批量删除
        toRemove.forEach(Workers::removeWorkerMapping);
    }

    private static void removeWorkerMapping(Unit worker) {
        Unit resource = workerToResourceMap.remove(worker);
    }

    private static boolean isGatheringOrder(Unit worker) {
        bwapi.Order order = worker.getOrder();
        if (Objects.nonNull(worker.getOrderTarget())
                && (worker.getOrderTarget().getType() == UnitType.Resource_Mineral_Field
                || worker.getOrderTarget().getType() == UnitType.Terran_Refinery)) {
            return true;
        }
        return order == bwapi.Order.MoveToGas ||            // 前往气矿
                order == bwapi.Order.WaitForGas ||          // 等待气矿
                order == bwapi.Order.HarvestGas ||          // 采集气体
                order == bwapi.Order.ReturnGas ||           // 返回气体
                order == bwapi.Order.MoveToMinerals ||      // 前往水晶
                order == bwapi.Order.WaitForMinerals ||     // 等待水晶
                order == bwapi.Order.MiningMinerals ||      // 采矿中
                order == bwapi.Order.ReturnMinerals ||      // 返回矿物
                order == bwapi.Order.ResetCollision ||      // 碰撞重置
                order == bwapi.Order.ResetHarvestCollision || // ✅ 采集碰撞重置
                order == bwapi.Order.Harvest1 ||            // ✅ 采集状态1
                order == bwapi.Order.Harvest2 ||            // ✅ 采集状态2
                order == bwapi.Order.Harvest3 ||            // ✅ 采集状态3
                order == bwapi.Order.Harvest4;              // ✅ 采集状态4
    }

    public static Unit getGatherTarget(Unit worker) {
        return isGatheringOrder(worker) ? worker.getOrderTarget() : null;
    }

    // ==================== 建筑工人管理 ====================

    public static void markAsExperiencedBuilder(Unit worker) {
        if (worker != null) {
            experiencedBuilders.add(worker);
        }
    }

    public static boolean isBuilder(Unit worker) {
        return experiencedBuilders.contains(worker);
    }

    public static Unit getBuilderWorker(Player player, Position target) {
        Unit bestBuilder = null;
        int bestDist = Integer.MAX_VALUE;

        for (Unit scv : experiencedBuilders) {
            if (scv == null || !scv.exists() || !scv.isIdle()) {
                continue;
            }
            if (!isReachable(scv)) {
                continue;
            }
            int dist = scv.getTilePosition().getApproxDistance(target.toTilePosition());
            if (dist < bestDist) {
                bestDist = dist;
                bestBuilder = scv;
            }
        }

        if (bestBuilder != null && bestDist <= 30) {
            return bestBuilder;
        }
        return getAWorker(player, target);
    }

    public static Unit getRepairWorker(Player player, Unit damagedBuilding) {
        for (Unit scv : experiencedBuilders) {
            if (scv != null && scv.exists() && scv.isIdle()) {
                return scv;
            }
        }
        return getBuilderWorker(player, damagedBuilding.getPosition());
    }

    private static boolean isReachable(Unit scv) {
        Unit mainBase = Bases.getMainBaseUnit();
        if (mainBase == null) return true;
        int distToBase = scv.getTilePosition().getApproxDistance(mainBase.getTilePosition());
        return distToBase <= 20;
    }

    public static void returnToBaseArea(Unit worker) {
        if (worker == null || !worker.exists()) {
            return;
        }
        Unit mainBase = Bases.getMainBaseUnit();
        if (mainBase == null) {
            return;
        }
        int distToBase = worker.getTilePosition().getApproxDistance(mainBase.getTilePosition());
        if (distToBase >= 5 && distToBase <= 10) {
            return;
        }
        Position target = calculateDirectionTowardsBase(worker, mainBase);
        worker.rightClick(target);
    }

    private static Position calculateDirectionTowardsBase(Unit worker, Unit base) {
        Position workerPos = worker.getPosition();
        Position basePos = base.getPosition();
        double dx = basePos.getX() - workerPos.getX();
        double dy = basePos.getY() - workerPos.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance == 0) {
            return basePos;
        }
        int targetDist = 8 * 32;
        int targetX = workerPos.getX() + (int) (dx / distance * targetDist);
        int targetY = workerPos.getY() + (int) (dy / distance * targetDist);
        return new Position(targetX, targetY);
    }

    // ==================== 工人选择 ====================

    public static int getWeightToChooseForBuild(Position buildPosition, Unit worker) {
        int weight = 0;
        if (worker.isCarryingMinerals() || worker.isCarryingGas()) {
            weight += 10000;
        }
        if (!worker.isIdle()) {
            weight += 1000;
        }
        weight += worker.getDistance(buildPosition);
        return weight;
    }

    public static Unit getAWorker(Player player, Position position) {
        List<Unit> workers = player.getUnits().stream()
                .filter(e -> e.getType().isWorker() && !e.isConstructing())
                .filter(e -> !Builds.getBuildingWorkers().contains(e))
                .filter(e -> !e.isGatheringGas() || e.isCarryingGas())
                .sorted(Comparator.comparingInt(o -> getWeightToChooseForBuild(position, o)))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(workers)) {
            workers = player.getUnits().stream()
                    .filter(e -> e.getType().isWorker())
                    .filter(e -> !Builds.getBuildingWorkers().contains(e))
                    .sorted(Comparator.comparingInt(o -> getWeightToChooseForBuild(position, o)))
                    .collect(Collectors.toList());
        }
        return CollectionUtils.isEmpty(workers) ? null : workers.get(0);
    }

    public static List<Unit> getWorkersByMineral(Player player, Unit base) {
        return base.getUnitsInRadius(12, (e) -> e.getType().isWorker()
                && isGatheringWorker(e)
                && e.getPlayer().equals(player));
    }

    // ==================== 采矿分配 ====================

    public static void goGatherLessLoader(Unit worker, Unit base) {
        Unit mineral = getClosestMineralsLessWorkers(worker, base);
        if (mineral != null) {
            worker.gather(mineral);
            workerToResourceMap.put(worker, mineral);
        }
    }

    public static void goGatherLessLoader(List<Unit> workers, Unit base) {
        workers.forEach(e -> {
            if (!experiencedBuilders.contains(e)) {
                goGatherLessLoader(e, base);
            }
        });
    }

    private static Set<Unit> getGatheringWorkersByMineral(Unit mineral) {
        return workerToResourceMap.entrySet().stream().filter(e -> e.getValue().equals(mineral)).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public static Unit getClosestMineralsLessWorkers(Unit worker, Unit baseUnit) {
        if (baseUnit == null) {
            return null;
        }
        Base base = getBaseFormBaseUnit(baseUnit);
        if (base == null) {
            return null;
        }
        List<Unit> mineralList = base.getMinerals().stream()
                .map(Neutral::getUnit)
                .sorted(Comparator.comparingDouble(e -> getMineralSortWeight(e, base.getCenter())))
                .collect(Collectors.toList());
        return mineralList.stream()
                .min(Comparator.comparingDouble(e -> getMineralSortWeight(e, base.getCenter()))).orElseGet(() -> getClosestMinerals(worker, base));
    }

    public static Unit getClosestMinerals(Unit worker, Base base) {
        if (base != null) {
            Mineral mineral = base.getMinerals().stream()
                    .min(Comparator.comparingInt(m -> worker.getDistance(m.getUnit().getPosition())))
                    .orElse(null);
            if (mineral != null) {
                return mineral.getUnit();
            }
        }

        Unit mineralUnit = Games.game.getMinerals().stream()
                .min(Comparator.comparingInt(worker::getDistance))
                .orElse(null);

        if (mineralUnit == null) {
            mineralUnit = Games.game.getGeysers().stream()
                    .filter(e -> e.isCompleted() && e.getPlayer() == worker.getPlayer())
                    .min(Comparator.comparingInt(worker::getDistance))
                    .orElse(null);
        }
        return mineralUnit;
    }

    public static double getMineralSortWeight(Unit mineral, Position position) {
        return Double.parseDouble(getGatheringWorkersByMineral(mineral).size() + "." + mineral.getDistance(position));
    }

    public static Base getBaseFormBaseUnit(Unit baseUnit) {
        return Games.bwem.getMap().getBases().stream()
                .min(Comparator.comparingInt(e -> baseUnit.getDistance(e.getLocation().toPosition())))
                .orElse(null);
    }

    // ==================== 缓存清理 ====================

    public static void clearCache() {
        workerToResourceMap.clear();
        experiencedBuilders.clear();
    }

    public static void goGatherGas(Unit worker, Unit refinery) {
        worker.gather(refinery);
        workerToResourceMap.put(worker, refinery);
    }
}
