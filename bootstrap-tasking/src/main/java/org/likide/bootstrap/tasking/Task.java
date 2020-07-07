package org.likide.bootstrap.tasking;

import java.util.List;
import java.util.function.Predicate;

public class Task<T> {

	private String description;

	private List<Stage<T>> stages;

}
