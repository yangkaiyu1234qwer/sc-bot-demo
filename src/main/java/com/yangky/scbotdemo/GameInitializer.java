package com.yangky.scbotdemo;

import bwapi.*;
import bwem.BWEM;
import com.yangky.scbotdemo.bwem.Bases;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Units;
import com.yangky.scbotdemo.bwem.Workers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * GameInitilizer
 *
 * @author yangky
 * @Date 2026/4/29 15:40
 */
public class GameInitializer {
    public static BWClient bwClient;
    public static Game game;
    public static boolean running = true;
    public static boolean initialized = false;  // 标记 BWEM 是否已初始化

    public static void start() {
        try {
            game = bwClient.getGame();
            Games.game = game;
            System.out.println("游戏开始，等待初始化...");
        } catch (Throwable t) {
            t.printStackTrace();
            running = false;
        }
    }

    /**
     * 标准模式初始化（使用 BWEM）
     */
    public static void setupInitialGame() {
        game.drawTextScreen(100, 100, "Hello World!");
        Player self = game.self();
        if (self == null) {
            System.out.println("警告：无法获取玩家信息");
            return;
        }
        List<Unit> units = self.getUnits();
        if (units == null || units.isEmpty()) {
            System.out.println("警告：没有检测到单位");
            return;
        }
        List<Unit> bases = Bases.getBaseCenterList(self);
        if (bases != null && !bases.isEmpty()) {
            bases.forEach(Units::unitCreated);
            Unit base = bases.get(0);
            Bases.trainWorkerIfIdle(base);
            List<Unit> workers = units.stream().filter(e -> e.getType().isWorker()).collect(Collectors.toList());
            Workers.goGatherLessLoader(workers, base);
            System.out.println("初始设置完成，基地数: " + bases.size() + ", 工人数: " + workers.size());
        } else {
            System.out.println("警告：没有检测到基地");
        }
    }

    // ... existing code ...
    public static void initializeBWEM() {
        try {
            Player self = game.self();
            if (self == null) {
                System.out.println("等待玩家信息...");
                return;
            }

            List<Unit> bases = Bases.getBaseCenterList(self);
            if (bases == null || bases.isEmpty()) {
                System.out.println("等待基地生成... (当前帧: " + game.getFrameCount() + ")");
                return;
            }

            System.out.println("检测到 " + bases.size() + " 个基地，开始初始化 BWEM...");

            BWEM bwem = new BWEM(game);
            bwem.initialize();
            Games.bwem = bwem;
            Games.init(bwem, game);
            initialized = true;
            System.out.println("BWEM 初始化成功！");
            setupInitialGame();
            List<Unit> units = game.self().getUnits();
            units.forEach(Units::unitCreated);
        } catch (IllegalStateException e) {
            System.err.println("BWEM 初始化失败: " + e.getMessage());
            System.err.println("当前地图不支持 BWEM，BOT 停止运行");
            running = false;
        } catch (Exception e) {
            System.err.println("初始化过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            running = false;
        }

        game.enableFlag(Flag.UserInput);
        System.out.println("用户输入开启");
    }
}
