/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 04 Dec 2022.
 */


package nl.das.nrtrunkmgr.handlers;

import java.io.IOException;
import java.io.StringReader;
import java.util.Deque;
import java.util.Map;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.svnactions.SvnActions;

/**
 *
 */
public class MergeAndCommitHandler implements HttpHandler {

	public static Logger log = LoggerFactory.getLogger(MergeAndCommitHandler.class);
	private static Properties props;
	private SvnActions svn;

	public MergeAndCommitHandler(Properties properties) {
		props = properties;
	}

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
        try {
    		this.svn = new SvnActions(props);
    		Map<String, Deque<String>> parms = exchange.getQueryParameters();
    		String msg = "";
    		Deque<String> msgs = parms.get("msg");
    		if (msgs != null) {
    			msg = msgs.pop();
    		}
        	String json = new String(exchange.getInputStream().readAllBytes());
        	JsonObject obj = Json.createReader(new StringReader(json)).readObject();
        	String branchName = obj.getString("brName");
    		// Get the changed files from the branch repo and write them into the trunk repo workdir
            String flow = this.svn.getBranchFlow(branchName, obj.getInt("revNo1"), false);
            String html = this.svn.getBranchUi("html", branchName, obj.getInt("revNo2"), false);
            String js = this.svn.getBranchUi("js", branchName, obj.getInt("revNo3"), false);
            String css = this.svn.getBranchUi("css", branchName, obj.getInt("revNo4"), false);
            if (obj.getInt("revNo1") != 0) {
            	if (flow.length() <= 10) {
            		throw new IOException("flow content is too short: '" + flow + "'");
            	}
				this.svn.updateFlow(flow);
            }
            if (obj.getInt("revNo2") != 0) {
            	if (html.length() <= 10) {
            		throw new IOException("html content is too short: '" + html + "'");
            	}
				this.svn.updateUi("html", html);
            }
            if (obj.getInt("revNo3") != 0) {
            	if (js.length() <= 10) {
            		throw new IOException("js content is too short: '" + js + "'");
            	}
				this.svn.updateUi("js", js);
            }
            if (obj.getInt("revNo4") != 0) {
            	if (css.length() <= 10) {
            		throw new IOException("css content is too short: '" + css + "'");
            	}
				this.svn.updateUi("css", css);
            }
    		json = this.svn.commit(msg) + "";
            System.out.println("1st commit");
     		// Register the merge
    		this.svn.merge(false, branchName);
            System.out.println("merged");
            // Then commit the trunk
    		json = this.svn.commit(msg) + "";
            System.out.println("2nd commit");
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
			exchange.getResponseSender().send(json);
		} catch (IOException | SVNException e) {
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
			exchange.getResponseSender().send(e.getMessage());
        }
	}

}
