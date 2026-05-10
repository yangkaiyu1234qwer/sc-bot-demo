package com.yangky.scbotdemo.bwem.build.handler;

import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * WaitingHandler
 *
 * @author yangky
 * @Date 2026/5/10 7:52
 */
@Component
public class WaitingHandler extends StateHandler {
    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.WAITING;
    }

    @Override
    public void process(Task task) {
        System.out.println("[DEBUG] WaitingHandler 处理任务: " + task.getIdempotentNo());
        task.setStatus(TaskStatus.ASSIGN_SCV);
    }

}
