package org.likide.bootstrap.tasking.stage;

import java.util.Arrays;

public class StageExecutor<T> implements IStageExecutor<T> {

	@Override
	public void execute(Stage<T> stage, StageControl<T> control, IStageListener<? super T> listener) {
		listener.onBegin(stage, control);
		if (stage.getCondition() == null || stage.getCondition().test(control.getPayload())) {
			control.preventSkip();
			listener.onBeforeExecute(stage, control);
			try {
				stage.getHandler().apply(control);
				listener.onAfterExecute(stage, control);
			} catch (RuntimeException e) {
				control.throwable(e);
				listener.onBeforeExceptionHandling(stage, control);
				handleException(stage, control, e, listener);
				listener.onAfterExceptionHandling(stage, control);
			}
			listener.onEnd(stage, control);
			switch (control.getBehavior()) {
			case BREAK:
				listener.onBreak(stage, control);
				break;
			case CONTINUE:
				listener.onContinue(stage, control);
				break;
			case EXIT:
				listener.onExit(stage, control);
				break;
			case SKIP:
				throw new IllegalStateException("Skip should not be set here");
			default:
				throw new IllegalStateException(String.format("Unknown behavior: %s", control.getBehavior()));
			
			}
		} else {
			control.doSkip();
			listener.onEnd(stage, control);
			listener.onSkip(stage, control);
		}
	}

	private <T> void handleException(Stage<T> stage, StageControl<T> control, RuntimeException e, IStageListener<? super T> listener) {
		if (stage.getExceptionHandler() == null) {
			control.doExit();
			return;
		}
		try {
			stage.getExceptionHandler().accept(e, control);
		} catch (RuntimeException re) {
			if (Arrays.stream(e.getSuppressed()).noneMatch(e::equals)) {
				re.addSuppressed(e);
			}
			control.throwable(re);
			control.doExit();
		}
	}

}
