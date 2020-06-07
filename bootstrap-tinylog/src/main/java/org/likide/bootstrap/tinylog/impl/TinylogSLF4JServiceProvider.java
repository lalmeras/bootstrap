package org.likide.bootstrap.tinylog.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;
import org.tinylog.slf4j.TinylogLoggerFactory;
import org.tinylog.slf4j.TinylogMdcAdapter;

public class TinylogSLF4JServiceProvider implements SLF4JServiceProvider {

	public static final String REQUESTED_API_VERSION = "1.8.99";

	private TinylogLoggerFactory loggerFactory;
	private BasicMarkerFactory markerFactory;
	private TinylogMdcAdapter mdcAdapter;

	@Override
	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return mdcAdapter;
	}

	@Override
	public String getRequesteApiVersion() {
		return REQUESTED_API_VERSION;
	}

	@Override
	public void initialize() {
		loggerFactory = new TinylogLoggerFactory();
		markerFactory = new BasicMarkerFactory();
		mdcAdapter = new TinylogMdcAdapter();
	}

}
