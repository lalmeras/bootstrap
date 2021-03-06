package org.likide.bootstrap.tasking.stage;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface IStage<T> {

	IStage<T> shortName(String shortName);

	IStage<T> description(String description);

	IStage<T> loggerName(String loggerName);

	IStage<T> condition(Predicate<T> condition);

	IStage<T> handler(UnaryOperator<IStageControl<T>> handler);

	IStage<T> parent(Stage<T> parent);

	IStage<T> up();

	StageCollection<T> stages();

	StageFork<T> fork();

	IStage<T> onException(BiConsumer<Throwable, IStageControl<T>> handler);

}
