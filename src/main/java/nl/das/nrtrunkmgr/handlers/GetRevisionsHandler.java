/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 20 Nov 2022.
 */


package nl.das.nrtrunkmgr.handlers;

import java.util.Deque;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.nrtrunkmgr.Utils;
import nl.das.nrtrunkmgr.model.TrunkInfo;
import nl.das.svnactions.SvnActions;

/**
 *
 */
public class GetRevisionsHandler implements HttpHandler {

	public static Logger log = LoggerFactory.getLogger(GetRevisionsHandler.class);
	private static Properties props;
	private SvnActions svn;

	public GetRevisionsHandler(Properties properties) {
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
				String branchName = "";
				Map<String, Deque<String>> parms = exchange.getQueryParameters();
				Deque<String> req= parms.get("branch");
				if (req != null) {
					branchName = req.pop();
				} else {
					exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
					exchange.getResponseSender().send("No branch name given.");
				}
				TrunkInfo ti = new TrunkInfo();
				ti.setUptodate(this.svn.isWCDirty());
				ti.setBranchName(branchName);
				ti.setRemoteUrl(this.svn.getWCUrl());
				ti.setWcLocation(props.getProperty("workdir"));
				ti.setLatestTrunkRevs(this.svn.getLatestTrunkRevisions());
				ti.setCurrentTrunkRev(this.svn.getLatestTrunkRevision());
				ti.setLatestBranchRevs(this.svn.getLatestBranchRevisions(branchName));
				ti.setCurrentBranchRev(this.svn.getLatestBranchRevision(branchName));
				ti.setHighestTrunkRevInBranch(this.svn.getLatestTrunkRevInBranch(false, branchName));
				String json = Utils.parser().toJson(ti);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
				exchange.getResponseSender().send(json);
			} catch (SVNException e) {
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
				exchange.getResponseSender().send(e.getMessage());
			}
		}
	}

}
