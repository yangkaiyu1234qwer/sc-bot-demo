package com.yangky.scbotdemo.bwem.build.handler;

import com.yangky.scbotdemo.bwem.Actions;
import com.yangky.scbotdemo.bwem.Games;
import com.yangky.scbotdemo.bwem.Workers;
import com.yangky.scbotdemo.bwem.build.StateHandler;
import com.yangky.scbotdemo.bwem.build.Task;
import com.yangky.scbotdemo.bwem.build.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * PositionHandler
 *
 * @author yangky
 * @Date 2026/5/10 7:55
 */
@Component
public class AssignScvHandler extends StateHandler {
    @Override
    public TaskStatus accessStatus() {
        return TaskStatus.ASSIGN_SCV;
    }

    @Override
    public void process(Task task) {
        System.out.println("[DEBUG] AssignScvHandler 处理任务: " + task.getIdempotentNo());
        if (task.getWorker() == null || !task.getWorker().exists()) {
            task.setWorker(Workers.getAWorker(task.getPosition().toPosition()));
            Workers.markAsExperiencedBuilder(task.getWorker());
        }
        task.setStatus(TaskStatus.MOVING);
        task.getWorker().stop();
        Actions.smartMove(task.getWorker(), task.getPosition().toPosition());
    }
}
