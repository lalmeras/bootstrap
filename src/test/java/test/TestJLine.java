package test;

import java.io.IOException;

import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

public class TestJLine {

	@Test
	public void testJLine() throws IOException {
		TerminalBuilder.terminal();
	}

}
