package com.yangky.scbotdemo.bwem.build.handler;

import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * RetryingHandler
 *
 * @author yangky
 * @Date 2026/5/10 7:49
 */
@Component
public class RetryingHandler extends StateHandler {
    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.COMPLETED;
    }

    @Override
    public void process(Task task) {
        System.out.println("[DEBUG] RetryingHandler 处理任务: " + task.getIdempotentNo());
        // 模拟重试逻辑
        if (task.getRetryCount() < 3) {
            task.setRetryCount(task.getRetryCount() + 1);
            task.setStatus(TaskStatus.WAITING);
        } else {
            task.setStatus(TaskStatus.FAILED);
        }
    }
}
