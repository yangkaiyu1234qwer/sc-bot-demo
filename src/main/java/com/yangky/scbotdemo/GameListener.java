package com.yangky.scbotdemo;

import bwapi.*;
import com.yangky.scbotdemo.bwem.*;
import com.yangky.scbotdemo.bwem.walloff.WallOffExecutor;
import com.yangky.scbotdemo.listner.created.UnitCreatedListenerChain;
import com.yangky.scbotdemo.listner.destroy.UnitDestroyListenerChain;
import com.yangky.scbotdemo.onframe.OnFrameChain;
import com.yangky.scbotdemo.util.Positions;
import com.yangky.scbotdemo.util.Printer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * GameListner
 *
 * @author yangky
 * @Date 2026/4/27 19:10
 */
@Component
public class GameListener extends DefaultBWListener implements CommandLineRunner {
    @Override
    public void onStart() {
        GameInitializer.start();
        WallOffExecutor.reset();
    }

    @Override
    public void onUnitMorph(Unit unit) {
        super.onUnitMorph(unit);
        // 检查游戏是否已结束
        Game game = Games.game;
        if (game == null || !game.isReplay() && game.getFrameCount() < 0) {
            return;
        }
        if (unit.getType().isBuilding()) {
            Units.unitCreated(unit);
        }
    }

    @Override
    public void onFrame() {
        try {
            Game game = Games.game;
            // 检查游戏是否已结束
            if (game == null || !game.isReplay() && game.getFrameCount() < 0) {
                return;
            }
            // 只在未初始化且 BWEM 可用时尝试初始化
            if (!GameInitializer.initialized && Games.bwem == null) {
                GameInitializer.initializeBWEM();
                return;
            }
            if (Games.bwem != null) {
                OnFrameChain.dispatch(game.getFrameCount());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            GameInitializer.running = false;
        }
    }

    @Override
    public void onUnitDestroy(Unit unit) {
        try {
            if (unit == null || Games.game == null || unit.getPlayer() == null || unit.getPlayer() != Games.game.self()) {
                return;
            }
            super.onUnitDestroy(unit);
            Units.destroyUnit(unit);
            UnitDestroyListenerChain.dispatch(unit);
        } catch (Throwable t) {
            t.printStackTrace();
            GameInitializer.running = false;
        }
    }

    @Override
    public void onUnitCreate(Unit unit) {
        try {
            super.onUnitCreate(unit);
            if (unit == null || Games.game == null
                    || unit.getPlayer() == null
                    || unit.getPlayer() != Games.game.self()
                    || Games.bwem == null
                    || !unit.exists()
            ) {
                return;
            }
            // 记录新单位（自动去重）
            Units.unitCreated(unit);
            UnitCreatedListenerChain.dispatch(unit);
        } catch (Throwable t) {
            t.printStackTrace();
            GameInitializer.running = false;
        }
    }

    @Override
    public void onUnitComplete(Unit unit) {
        super.onUnitCreate(unit);
    }

    @Override
    public void onEnd(boolean isWinner) {
        try {
            super.onEnd(isWinner);
            // ✅ 只设置标志位，不做任何耗时操作
            GameInitializer.initialized = false;
            // ✅ 不清理资源，留到 startGame() 返回后再清理
            System.out.println("\n========== 游戏结束（" + (isWinner ? "胜利" : "失败") + "）==========");
            System.out.println("========== 请在星际中选择继续游戏 ==========\n");
        } catch (Throwable t) {
            System.err.println("onEnd 处理异常: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SuppressWarnings("BusyWait")
    @Override
    public void run(String... args) {
        GameInitializer.bwClient = new BWClient(this);
        // 无限循环，支持重连
        do {
            try {
                Thread.sleep(200L);
                System.out.println("正在连接到 BWAPI...");
                GameInitializer.bwClient.startGame();
                // ✅ startGame() 返回后，说明 BWAPI 连接已释放，此时再清理资源
                System.out.println("游戏结束，等待重新连接...");
                cleanupAfterGame();
            } catch (Exception e) {
                System.out.println("连接失败，3秒后重试: " + e.getMessage());
                e.printStackTrace();
                // 发生异常时也要等待一下再重试
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } while (GameInitializer.running);
        System.out.println("程序正常退出");
    }

    /**
     * 游戏结束后清理资源（在 startGame() 返回后调用）
     */
    private void cleanupAfterGame() {
        System.out.println("[清理] 开始清理游戏资源...");

        // 1. 打印建筑信息（此时游戏对象仍然有效）
        try {
            Printer.printBuildingPositions(Games.game);
        } catch (Exception e) {
            System.err.println("打印建筑信息失败: " + e.getMessage());
        }

        // 2. 清理 BWEM 引用
        if (Games.bwem != null) {
            Games.bwem = null;
            System.out.println("[清理] BWEM 已清理");
        }

        // 3. 清理 Game 引用
        Games.game = null;
        GameInitializer.game = null;
        System.out.println("[清理] Game 引用已清理");

        // 4. 清理 Units 静态集合
        Units.clearAll();
        System.out.println("[清理] Units 集合已清理");

        // 5. 清理 Builds 任务列表
        Builds.clearAll();
        System.out.println("[清理] Builds 任务列表已清理");

        // 6. 清理 Workers 缓存
        Workers.clearCache();
        System.out.println("[清理] Workers 缓存已清理");

        // 7. 清理 WallOffExecutor 状态
        WallOffExecutor.reset();
        System.out.println("[清理] WallOffExecutor 已重置");

        // 8. 清理 Supplies 状态
        Supplies.reset();
        System.out.println("[清理] Supplies 状态已重置");

        Actions.clearWorkerTracking();
        System.out.println("[清理] 工人移动追踪已清理");

        Positions.resetFailedPositions();

        System.out.println("[清理] 资源清理完成\n");
    }
}


