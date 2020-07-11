package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.likide.bootstrap.tasking.stage.IStageControl;
import org.likide.bootstrap.tasking.stage.IStageListener;
import org.likide.bootstrap.tasking.stage.Stage;
import org.likide.bootstrap.tasking.stage.StageBehavior;
import org.likide.bootstrap.tasking.stage.StageControl;
import org.likide.bootstrap.tasking.stage.StageExecutor;
import org.mockito.AdditionalAnswers;

import com.google.common.base.Suppliers;

public class TestStage {

	@SuppressWarnings("unchecked")
	private Supplier<IStageListener<Object>> listener = Suppliers.memoize(() -> mock(IStageListener.class));
	@SuppressWarnings("unchecked")
	private Supplier<UnaryOperator<IStageControl<Payload>>> handler = Suppliers.memoize(() -> mock(UnaryOperator.class));
	@SuppressWarnings("unchecked")
	private Supplier<BiConsumer<Throwable, IStageControl<Payload>>> exceptionHandler = Suppliers.memoize(() -> mock(BiConsumer.class));

	@Test
	public void executeStage() {
		// handler is called with payload
		Stage<Payload> stage = new Stage<Payload>();
		Payload payload = new Payload();
		stage.handler(handler.get());
		StageControl<Payload> control = new StageControl<>(payload);
		new StageExecutor().execute(stage, control, listener.get());
		
		verify(handler.get()).apply(control);
		verify(listener.get()).onBegin(stage, control);
		verify(listener.get()).onBeforeExecute(stage, control);
		verify(listener.get()).onAfterExecute(stage, control);
		verify(listener.get()).onEnd(stage, control);
		verify(listener.get()).onContinue(stage, control);
		verifyNoMoreInteractions(listener.get());
	}

	@Test
	public void throwingStage() {
		// mocking: execution throws exception
		when(handler.get().apply(any())).thenThrow(CustomException.class);
		
		// execution
		Stage<Payload> stage = new Stage<Payload>();
		Payload payload = new Payload();
		stage.handler(handler.get());
		StageControl<Payload> control = new StageControl<>(payload);
		new StageExecutor().execute(stage, control, listener.get());
		
		// assertions
		assertThat(control.getThrowable())
			.as("check throwable").isInstanceOf(CustomException.class);
		assertThat(control.getBehavior())
			.as("check throwable").isEqualTo(StageBehavior.EXIT);
		verify(listener.get()).onBegin(stage, control);
		verify(listener.get()).onBeforeExecute(stage, control);
		verify(listener.get()).onBeforeExceptionHandling(stage, control);
		verify(listener.get()).onAfterExceptionHandling(stage, control);
		verify(listener.get()).onEnd(stage, control);
		verify(listener.get()).onExit(stage, control);
		verifyNoMoreInteractions(listener.get());
	}

	@Test
	public void handledThrowingStage() {
		// mocking: execution throws exception, exception handler triggers continue
		// throws an exception during execution
		when(handler.get().apply(any())).thenThrow(CustomException.class);
		// handle exception with a continue
		doAnswer(AdditionalAnswers.<Throwable, IStageControl<Payload>>answerVoid((a, b) -> b.doContinue()))
			.when(exceptionHandler.get()).accept(any(), any());
		
		// execution
		Stage<Payload> stage = new Stage<Payload>();
		Payload payload = new Payload();
		stage.handler(handler.get());
		stage.onException(exceptionHandler.get());
		StageControl<Payload> control = new StageControl<>(payload);
		new StageExecutor().execute(stage, control, listener.get());
		
		// assertions
		verify(exceptionHandler.get()).accept(any(CustomException.class), eq(control));
		assertThat(control.getThrowable())
			.as("check wrapped").isInstanceOf(CustomException.class);
		assertThat(control.getBehavior())
			.as("check throwable").isEqualTo(StageBehavior.CONTINUE);
		verify(listener.get()).onBegin(stage, control);
		verify(listener.get()).onBeforeExecute(stage, control);
		verify(listener.get()).onBeforeExceptionHandling(stage, control);
		verify(listener.get()).onAfterExceptionHandling(stage, control);
		verify(listener.get()).onEnd(stage, control);
		verify(listener.get()).onContinue(stage, control);
		verifyNoMoreInteractions(listener.get());
	}

	@Test
	public void throwingManagedStage() {
		// exception is thrown and managed
	}

	private class Payload {
	}

	@SuppressWarnings("serial")
	private class CustomException extends RuntimeException {
	}

}
