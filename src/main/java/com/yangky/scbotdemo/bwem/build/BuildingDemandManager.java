package com.yangky.scbotdemo.bwem.build;

import bwapi.UnitType;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BuildingDemandManager - 建筑需求管理器
 * <p>
 * 核心职责：
 * 1. 接收各模块的建筑需求声明
 * 2. 自动计算差值（目标数量 - 当前数量）
 * 3. 自动创建对应的建造任务
 * <p>
 * 使用方式：
 * - 初始化时调用 declare() 声明需求
 * - 每帧调用 syncToTasks() 同步需求到任务
 *
 * @author yangky
 * @Date 2026/5/12
 */
public class BuildingDemandManager {

    private static final Map<String, BuildingDemand> demands = new ConcurrentHashMap<>();

    public static void declare(BuildingDemand demand) {
        if (demand.getBuildingType() == null || demand.getTargetCount() <= 0) {
            return;
        }
        String demandKey = generateDemandKey(demand.getBuildingType());
        demands.put(demandKey, demand);
    }

    /**
     * 同步需求到任务（每帧调用）
     * <p>
     * 执行流程：
     * 1. 遍历所有声明的需求
     * 2. 检查建造时机（如果有）
     * 3. 统计当前数量（已完成 + 正在建造）
     * 4. 计算差值，自动创建缺失的任务
     */
    public static void syncToTasks() {
        if (demands.isEmpty()) {
            return;
        }
        for (BuildingDemand demand : demands.values()) {
            try {
                syncSingleDemand(demand);
            } catch (Exception e) {
                System.out.println("[BuildingDemandManager] ✗ 同步需求失败: " + demand.getBuildingType()
                        + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 同步单个需求
     */
    private static void syncSingleDemand(BuildingDemand demand) {
        UnitType type = demand.getBuildingType();
        int targetCount = demand.getTargetCount();
        // 1. 检查建造时机
        if (demand.getBuildTiming() != null) {
            if (!demand.getBuildTiming().accept(Games.game)) {
                return;
            }
        }
        // 2. 统计当前数量
        int completed = Units.getSelfUnits(type).size();
        int building = BuildExecutor.getCountByBuildingType(type);
        int currentTotal = completed + building;
        // 3. 计算差值
        int needToBuild = targetCount - currentTotal;
        if (needToBuild <= 0) {
            return;
        }
        // 4. 创建 Task
        for (int i = 0; i < needToBuild; i++) {
            String idempotentNo = generateIdempotentNo(type, i);
            // 避免重复创建
            if (BuildExecutor.getTaskByIdempotent(idempotentNo) != null) {
                continue;
            }
            // 创建 Task（带或不带回调）
            Task task;
            if (demand.getCallback() != null) {
                task = Task.of(idempotentNo, demand);
            } else {
                task = Task.of(idempotentNo, demand);
            }
            BuildExecutor.add(task);
            System.out.println("[BuildingDemandManager] ✓ 创建任务: " + idempotentNo
                    + " - " + type + " at " + demand.getPosition() + " (offset: " + demand.getXOffset() + "," + demand.getYOffset() + ")");
        }
    }

    /**
     * 生成需求的唯一键
     */
    private static String generateDemandKey(UnitType type) {
        return "demand_" + type.toString();
    }

    /**
     * 生成任务的幂等号
     */
    private static String generateIdempotentNo(UnitType type, int index) {
        return "demand_" + type.toString() + "_" + index;
    }

    /**
     * 清除所有需求（游戏重置时调用）
     */
    public static void clear() {
        demands.clear();
        System.out.println("[BuildingDemandManager] 已清除所有需求");
    }

    /**
     * 获取当前声明的需求数量
     */
    public static int getDemandCount() {
        return demands.size();
    }
}

