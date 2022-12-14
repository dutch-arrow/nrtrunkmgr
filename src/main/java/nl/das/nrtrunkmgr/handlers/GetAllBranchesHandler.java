/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 07 Nov 2022.
 */


package nl.das.nrtrunkmgr.handlers;

import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.nrtrunkmgr.Utils;
import nl.das.svnactions.SvnActions;

/**
 *
 */
public class GetAllBranchesHandler implements HttpHandler {

	public static Logger log = LoggerFactory.getLogger(GetAllBranchesHandler.class);
	private static Properties props;
	private SvnActions svn;

	public GetAllBranchesHandler(Properties properties) {
		props = properties;
	}

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
		props.setProperty("username",exchange.getRequestCookie("SvnUser").getValue());
		props.setProperty("password",exchange.getRequestCookie("SvnPwd").getValue());
		this.svn = new SvnActions(props);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		if (exchange.getRequestMethod().toString().equalsIgnoreCase("POST")) {
			try {
				List<String> branches = this.svn.getAllBranches();
		        int isDirty = this.svn.isWCDirty();
				String bjson = Utils.parser().toJson(branches);
				String json = "{\"hasChanges\":" + isDirty + ",\"curTrunkRev\":" + this.svn.getLatestTrunkRevision() + ",\"branches\":" + bjson + "}";
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(json);
			} catch (SVNException e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		}
	}

}
