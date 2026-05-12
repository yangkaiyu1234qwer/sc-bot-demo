//package com.yangky.scbotdemo.onframe;
//
//import bwapi.Player;
//import bwapi.TilePosition;
//import bwapi.Unit;
//import bwapi.UnitType;
//import com.yangky.scbotdemo.bwem.Bases;
//import com.yangky.scbotdemo.bwem.Games;
//import com.yangky.scbotdemo.bwem.build.BuildExecutor;
//import com.yangky.scbotdemo.bwem.build.BuildingPlacer;
//import com.yangky.scbotdemo.bwem.build.Task;
//import com.yangky.scbotdemo.bwem.region.RegionType;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//
///**
// * OnFrameForSupplyFallback - 卡人口兜底任务
// * <p>
// * 功能：检测到卡人口超过3分钟，强制在随机坐标造2个补给站
// *
// * @author yangky
// * @Date 2026/5/2
// */
//@Component
//public class OnFrameForSupplyFallback extends OnFrame {
//
//    private static final long STUCK_SUPPLY_THRESHOLD_MS = 90 * 1000; // 1分半
//
//    private Long stuckSupplyStartTime = null;  // 卡人口开始时间
//    private int lastSupplyTotal = 0;            // 上次的人口上限
//    private boolean fallbackTriggered = false;  // 是否已触发兜底
//
//    private final Random random = new Random();
//
//    @Override
//    public Integer getInterval() {
//        return 10; // 每10帧检查一次
//    }
//
//    @Override
//    public void onFrame(Integer frame) {
//        Player player = Games.game.self();
//        if (player == null) return;
//        if (frame < 4000) {
//            return;
//        }
//        int currentSupplyTotal = player.supplyTotal();
//        int supplyUsed = player.supplyUsed();
//        // 重置逻辑：如果人口上限增加了，说明不卡了
//        if (currentSupplyTotal > lastSupplyTotal) {
//            if (stuckSupplyStartTime != null) {
//                System.out.println("[SupplyFallback] 人口上限增加，重置卡人口计时器: " + lastSupplyTotal + " -> " + currentSupplyTotal);
//            }
//            stuckSupplyStartTime = null;
//            fallbackTriggered = false;
//            lastSupplyTotal = currentSupplyTotal;
//            return;
//        }
//        lastSupplyTotal = currentSupplyTotal;
//        // 检测是否卡人口（有 deficit 且持续存在）
//        if (supplyUsed >= currentSupplyTotal) {
//            if (stuckSupplyStartTime == null) {
//                stuckSupplyStartTime = System.currentTimeMillis();
//                System.out.println("[SupplyFallback] 检测到卡人口，开始计时... ");
//            } else {
//                long stuckDuration = System.currentTimeMillis() - stuckSupplyStartTime;
//                // 超过1分半，触发兜底
//                if (stuckDuration >= STUCK_SUPPLY_THRESHOLD_MS && !fallbackTriggered) {
//                    System.out.println("[SupplyFallback] ⚠️ 卡人口超过1分半！触发兜底任务...");
//                    triggerFallbackSupply(player);
//                    fallbackTriggered = true;
//                } else if (stuckDuration % 30000 < 100) { // 每30秒打印一次进度
//                    System.out.println("[SupplyFallback] 卡人口持续: " + (stuckDuration / 1000) + "秒");
//                }
//            }
//        } else {
//            // 没有 deficit，重置计时器
//            if (stuckSupplyStartTime != null) {
//                System.out.println("[SupplyFallback] 人口充足，重置计时器");
//            }
//            stuckSupplyStartTime = null;
//            fallbackTriggered = false;
//        }
//    }
//
//    /**
//     * 触发兜底 Supply 建造
//     */
//    private void triggerFallbackSupply(Player player) {
//        Unit mainBase = Bases.getMainBaseUnit();
//        if (mainBase == null) {
//            System.out.println("[SupplyFallback] ✗ 未找到主基地，兜底失败");
//            return;
//        }
//        for (int i = 0; i < 2; i++) {
//            String taskId = "fallback_supply_" + System.currentTimeMillis() + "_" + i;
//            Task task = Task.ofSupplyDepot(taskId, null);
//            List<RegionType> regionTypeList = new ArrayList<>();
//            regionTypeList.add(RegionType.EDGE);
//            task.setPosition(BuildingPlacer.findPosition(UnitType.Terran_Supply_Depot, regionTypeList, 0, 0, true));
//            BuildExecutor.add(task);
//            System.out.println("[SupplyFallback] ✓ 兜底 Supply 任务已添加 ");
//        }
//    }
//}
