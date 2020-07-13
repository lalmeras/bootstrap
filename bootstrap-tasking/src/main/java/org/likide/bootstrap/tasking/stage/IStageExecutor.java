package org.likide.bootstrap.tasking.stage;

public interface IStageExecutor<T> {

	void execute(Stage<T> stage, StageControl<T> control, IStageListener<? super T> listener);

}
