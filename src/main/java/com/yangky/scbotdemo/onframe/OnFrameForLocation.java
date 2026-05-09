package com.yangky.scbotdemo.onframe;

import com.yangky.scbotdemo.bwem.Locations;
import com.yangky.scbotdemo.bwem.region.RegionsClassifier;
import org.springframework.stereotype.Component;


/**
 * 地图区域分析调试绘制
 * 只在初始化完成后执行一次，绘制区域分类结果
 *
 * @author yangky
 * @Date 2026/5/2
 */
@Component
public class OnFrameForLocation extends OnFrame {
    private static final boolean drawn = false;
    private final int frameCount = 0;

    @Override
    public Integer getInterval() {
        return 1;
    }

    @Override
    public void onFrame(Integer frame) {
        if (!Locations.isInitialized()) {
            Locations.initialize();
            return;
        }
//        // 添加调试信息
//        int mapSize = Locations.getLocationMap().size();
//        if (mapSize == 0) {
//            if (frameCount <= 5) {
//                System.out.println("[LocationDebug] 警告：locationMap为空，无法绘制");
//            }
//            return;
//        }
//        drawLocationDebug();
//        // 暂时不设置drawn = true，让每帧都绘制
//        if (frameCount == 1) {
//            System.out.println("[LocationDebug] 调试信息已绘制完成（仅首次输出）");
//        }
    }
//
//    private void drawLocationDebug() {
//        int drawCount = 0;
//        for (Map.Entry<TilePosition, Location> entry : Locations.getLocationMap().entrySet()) {
//            TilePosition pos = entry.getKey();
//            Location loc = entry.getValue();
//            Position pixelPos = pos.toPosition().add(new Position(16, 16));
//
//            switch (loc.getRegionType()) {
//                case MINERAL:
//                    Games.game.drawCircleMap(pixelPos, 6, new Color(0, 0, 255));
//                    break;
//                case CHOKE_POINT:
//                    Games.game.drawBoxMap(
//                            pixelPos.getX() - 6, pixelPos.getY() - 6,
//                            pixelPos.getX() + 6, pixelPos.getY() + 6,
//                            new Color(255, 0, 0)
//                    );
//                    break;
//                case EDGE:
//                    drawTriangle(pixelPos, 6, new Color(255, 255, 0));
//                    break;
//                case CENTRAL:
//                    Games.game.drawCircleMap(pixelPos, 4, new Color(0, 255, 0));
//                    break;
//                case BOUNDARY:
//                    drawXMark(pixelPos, 10, new Color(255, 165, 0));
//                    break;
//                default:
//                    break;
//            }
//            drawCount++;
//        }
//    }
//
//    /**
//     * 绘制三角形的辅助方法
//     */
//    private void drawTriangle(Position center, int size, Color color) {
//        // 计算三角形的三个顶点（边长 2*size）
//        int x1 = center.getX();
//        int y1 = center.getY() - size; // 顶点
//        int x2 = center.getX() - size;
//        int y2 = center.getY() + size; // 左下点
//        int x3 = center.getX() + size;
//        int y3 = center.getY() + size; // 右下点
//        // 绘制三条边，加粗线条
//        Games.game.drawLineMap(x1, y1, x2, y2, color);
//        Games.game.drawLineMap(x2, y2, x3, y3, color);
//        Games.game.drawLineMap(x3, y3, x1, y1, color);
//    }
//
//    /**
//     * 绘制 X 标记的辅助方法
//     */
//    private void drawXMark(Position center, int size, Color color) {
//        // 绘制两条交叉线形成 X，尺寸放大
//        int halfSize = size / 2;
//        Games.game.drawLineMap(
//                center.getX() - halfSize, center.getY() - halfSize,
//                center.getX() + halfSize, center.getY() + halfSize,
//                color
//        );
//        Games.game.drawLineMap(
//                center.getX() - halfSize, center.getY() + halfSize,
//                center.getX() + halfSize, center.getY() - halfSize,
//                color
//        );
//    }

}
