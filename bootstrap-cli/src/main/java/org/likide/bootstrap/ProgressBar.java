package org.likide.bootstrap;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongBinaryOperator;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressBar implements Consumer<Long> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProgressBar.class);

	private final ScheduledExecutorService progressExecutor;
	private final Terminal terminal;
	private final boolean terminalOutput;
	private final LongBinaryOperator accumulator;

	private Long current;
	private Long previous;
	private Long total;
	private Integer terminalWidth;

	public ProgressBar(Terminal terminal, Long total, LongBinaryOperator accumulator) {
		progressExecutor = Executors.newSingleThreadScheduledExecutor();
		this.total = total;
		this.terminal = terminal;
		this.previous = 0l;
		this.accumulator = accumulator;
		this.current = 0l;
		if (terminal != null && terminal.getWidth() != 0) {
			terminalWidth = terminal.getWidth();
		} else {
			terminalWidth = 80;
		}
		if (terminal != null && terminal.getStringCapability(Capability.parm_left_cursor) != null) {
			terminalOutput = true;
		} else {
			terminalOutput = false;
		}
	}

	public ProgressBar start() {
		progressExecutor.scheduleAtFixedRate(this::refresh, 0, 100, TimeUnit.MILLISECONDS);
		return this;
	}
	
	public synchronized void stop() {
		try {
			progressExecutor.shutdownNow();
			if (!progressExecutor.awaitTermination(10, TimeUnit.SECONDS) && !progressExecutor.isTerminated()) {
				LOGGER.warn("Progress bar executor not terminated before timeout.");
			}
			refresh();
			System.out.println(); // NOSONAR
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void accept(Long current) {
		tick(current);
	}

	public synchronized void tick(Long current) {
		this.current = accumulator.applyAsLong(this.current, current);
	}

	private synchronized void refresh() {
		// check terminal size
		int width = this.terminalWidth;
		long localTotal = this.total;
		long currentCounter = this.current;
		int keep = 10;
		int maxChars = width - keep;
		
		// compute ratio
		// maxChars is width minus reserved chars for brackets and extra informations
		// currentChars is advancement in char length (progress bar)
		// ratio is a percentage
		int currentChars = (int) (maxChars * ((double) currentCounter / localTotal));
		int ratio = (int) (100 * ((double) currentCounter / localTotal));
		
		// ensure that maxChars does not overflow maxChars
		currentChars = Math.min(maxChars, currentChars);
		
		if (terminal != null && terminalOutput) {
			// print progress-bar if line clearing is available
			terminal.puts(Capability.parm_left_cursor, width);
			terminal.flush();
			if (currentCounter < localTotal) {
				terminal.writer().append(String.format("[%s>%s] %3d%%",
						"-".repeat(currentChars),
						" ".repeat(maxChars - currentChars - keep),
						ratio)).flush();;
			} else {
				terminal.writer().append(String.format("[%s] %3d%%", "-".repeat(maxChars - keep + 1), ratio)).flush();
			}
		} else {
			// print dot-bar if line clearing is not available
			long localPrevious = this.previous;
			int previousChars = (int) (maxChars * ((double) localPrevious / localTotal));
			System.out.print(".".repeat(currentChars - previousChars)); //NOSONAR java:S106
			this.previous = current;
		}
	}

}
