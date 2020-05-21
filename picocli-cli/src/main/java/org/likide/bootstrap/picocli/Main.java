package org.likide.bootstrap.picocli;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
		name = "picocli",
		mixinStandardHelpOptions = true
)
public class Main implements Callable<Integer> {

	public static void main(String[] args) {
		Integer status = new CommandLine(new Main()).execute(args);
		System.exit(status);
	}

	@Override
	public Integer call() throws Exception {
		System.out.println("Hello world!");
		return 0;
	}

}
