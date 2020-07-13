package test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.likide.bootstrap.tasking.stage.IStageExecutor;
import org.likide.bootstrap.tasking.stage.Stage;
import org.likide.bootstrap.tasking.task.Task;
import org.likide.bootstrap.tasking.task.TaskControl;
import org.likide.bootstrap.tasking.task.TaskExecutor;

public class TestTask {

	private Object payload = mock(Object.class);
	@SuppressWarnings("unchecked")
	private Stage<Object> stage = mock(Stage.class);
	private TaskControl<Object> control = spy(new TaskControl<>(payload));
	@SuppressWarnings("unchecked")
	private IStageExecutor<Object> executor = mock(IStageExecutor.class);

	@Test
	public void testTaskIsExecuted() {
		// A task executes its stage
		TaskExecutor<Object> taskExecutor = new TaskExecutor<>();
		Task<Object> task = new Task<>("dummy task", stage);
		
		taskExecutor.execute(task, control, executor);
		
		verify(executor).execute(eq(stage), any(), any());
	}

	@Test
	public void testThrowingExecutionReportsAnError() {
		// A task executes its stage
		TaskExecutor<Object> taskExecutor = new TaskExecutor<>();
		Task<Object> task = new Task<>("dummy task", stage);
		doThrow(RuntimeException.class).when(executor).execute(eq(stage), any(), any());
		
		taskExecutor.execute(task, control, executor);
		
		verify(executor).execute(eq(stage), any(), any());
		verify(control).getPayload();
		verify(control).exitOnError(eq(stage), any(RuntimeException.class), eq(1));
	}

	/**
	 * TaskExecutor main contract :
	 * - takes 
	 */

}
