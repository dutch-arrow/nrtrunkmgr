/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 07 Nov 2022.
 */


package nl.das.nrtrunkmgr.handlers;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import nl.das.svnactions.SvnActions;

/**
 *
 */
public class ChangedUiHandler implements HttpHandler {

	public static Logger log = LoggerFactory.getLogger(ChangedUiHandler.class);
	private static Properties props;
	private SvnActions svn;

	public ChangedUiHandler(Properties properties) {
		props = properties;
	}

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		// TODO Auto-generated method stub

	}

}
