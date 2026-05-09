package com.yangky.scbotdemo.bwem.build;

import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.TreeSet;

/**
 * BuildExecutor
 *
 * @author yangky
 * @Date 2026/5/9 22:45
 */
public class BuildExecutor {
    private static TreeSet<Task> tasks;
    private static Set<StateHandler> handlerList;

    public static void clear() {
        tasks.clear();
    }

    public void handleRegister(StateHandler handler) {
        handlerList.add(handler);
    }

    public static void add(Task task) {
        System.out.println("[DEBUG] 尝试添加任务: " + task.getIdempotentNo() + ", 当前任务数: " + tasks.stream().filter(e -> e.getStatus() != TaskStatus.COMPLETED).count());
        // ✅ 幂等检查
        if (tasks.stream().anyMatch(e -> StringUtils.equals(e.getIdempotentNo(), task.getIdempotentNo()))) {
            System.out.println("任务已存在，跳过: " + task.getIdempotentNo());
            return;
        }
        tasks.add(task);
    }

    public static void execute() {
        tasks.stream()
                .filter(e -> e.getStatus() != TaskStatus.COMPLETED).forEach(e -> {
                    handlerList.forEach(f -> {
                        if (f.accessStatus() == e.getStatus()) {
                            f.process(e);
                        }
                    });
                });
    }
}
