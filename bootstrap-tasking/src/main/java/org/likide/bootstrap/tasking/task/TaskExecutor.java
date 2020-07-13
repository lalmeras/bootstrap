package org.likide.bootstrap.tasking.task;

import org.likide.bootstrap.tasking.stage.DebugStageListener;
import org.likide.bootstrap.tasking.stage.IStageExecutor;
import org.likide.bootstrap.tasking.stage.StageControl;

public class TaskExecutor<T> {

	public void execute(Task<T> task, TaskControl<T> control, IStageExecutor<T> stageExecutor) {
		StageControl<T> stageControl = new StageControl<T>(control.getPayload());
		try {
			stageExecutor.execute(task.getStage(), stageControl, new DebugStageListener<T>());
		} catch (RuntimeException e) {
			control.exitOnError(task.getStage(), e, 1);
		}
	}

}
