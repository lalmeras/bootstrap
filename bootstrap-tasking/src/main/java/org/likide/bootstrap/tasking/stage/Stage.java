package org.likide.bootstrap.tasking.stage;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Stage<T> implements IStage<T> {

	private String shortName;

	private String description;

	private String loggerName = "DEBUG";

	private Predicate<T> condition;

	private UnaryOperator<IStageControl<T>> handler;

	private BiConsumer<Throwable, IStageControl<T>> exceptionHandler;

	private Stage<T> parent;

	private StageCollection<T> stages = new StageCollection<>(this);

	private StageFork<T> fork = new StageFork<>(this);

	@Override
	public IStage<T> shortName(String shortName) {
		this.shortName = shortName;
		return this;
	}

	@Override
	public IStage<T> description(String description) {
		this.description = description;
		return this;
	}

	@Override
	public IStage<T> loggerName(String loggerName) {
		this.loggerName = loggerName;
		return this;
	}

	@Override
	public IStage<T> condition(Predicate<T> condition) {
		this.condition = condition;
		return this;
	}

	@Override
	public IStage<T> handler(UnaryOperator<IStageControl<T>> handler) {
		this.handler = handler;
		return this;
	}

	@Override
	public IStage<T> parent(Stage<T> parent) {
		this.parent = parent;
		return this;
	}

	@Override
	public IStage<T> up() {
		if (parent == null) {
			throw new IllegalStateException(String.format("No parent on %s", this));
		}
		return parent;
	}

	@Override
	public StageCollection<T> stages() {
		return stages;
	}

	@Override
	public StageFork<T> fork() {
		return fork;
	}

	@Override
	public IStage<T> onException(BiConsumer<Throwable, IStageControl<T>> handler) {
		this.exceptionHandler = handler;
		return this;
	}

	public Stage<T> self() {
		return this;
	}

	public Predicate<T> getCondition() {
		return condition;
	}

	public UnaryOperator<IStageControl<T>> getHandler() {
		return handler;
	}

	public BiConsumer<Throwable, IStageControl<T>> getExceptionHandler() {
		return exceptionHandler;
	}

	public Stage<T> getParent() {
		return parent;
	}

	public StageCollection<T> getStages() {
		return stages;
	}

	public StageFork<T> getFork() {
		return fork;
	}

	public String getShortName() {
		return shortName;
	}

	public String getDescription() {
		return description;
	}

	public String getLoggerName() {
		return loggerName;
	}

}
