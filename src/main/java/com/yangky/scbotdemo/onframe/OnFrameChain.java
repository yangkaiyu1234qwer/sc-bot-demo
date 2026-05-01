package com.yangky.scbotdemo.onframe;

import bwapi.Player;
import com.yangky.scbotdemo.bwem.Games;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OnFrameChain
 *
 * @author yangky
 * @Date 2026/4/28 4:20
 */
public class OnFrameChain {
    private static final List<OnFrame> onFrames = new ArrayList<>();

    public static void addOnFrame(OnFrame onFrame) {
        onFrames.add(onFrame);
    }

    public static void dispatch(Integer frame) {
        List<OnFrame> list = onFrames.stream().filter(e -> e.apply(frame)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        Player self = Games.game.self();
        if (self.isNeutral() || self.isObserver() || self.isDefeated()) {
            return;
        }
        list.forEach(e -> {
            try {
                e.onFrame(frame);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
