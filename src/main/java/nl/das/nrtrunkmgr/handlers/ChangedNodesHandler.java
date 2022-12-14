/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 07 Nov 2022.
 */


package nl.das.nrtrunkmgr.handlers;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import nl.das.nrtrunkmgr.Utils;
import nl.das.nrtrunkmgr.model.FlowNode;
import nl.das.svnactions.SvnActions;

/**
 *
 */
public class ChangedNodesHandler implements HttpHandler {

	public static Logger log = LoggerFactory.getLogger(ChangedNodesHandler.class);
	private static Properties props;
	private SvnActions svn;

	public ChangedNodesHandler(Properties properties) {
		props = properties;
	}

	@Override
	public void handleRequest (HttpServerExchange exchange) throws Exception {
        try {
    		this.svn = new SvnActions(props);
        	String json = new String(exchange.getInputStream().readAllBytes());
        	JsonObject obj = Json.createReader(new StringReader(json)).readObject();
            String bflow = this.svn.getBranchFlow(obj.getString("brName"), obj.getInt("revNo1"), false);
            String tflow = this.svn.getTrunkFlow(obj.getInt("revNo2"));
            json = getDiffFlows(bflow, tflow);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
			exchange.getResponseSender().send(json);
		} catch (IOException | SVNException e) {
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
			exchange.getResponseSender().send(e.getMessage());
        }
	}
    private String getDiffFlows(String branchFlow, String trunkFlow) throws UnsupportedEncodingException {
        List<FlowNode> brFlowNodes = getFlowNodes(branchFlow);
        List<FlowNode> lvFlowNodes = getFlowNodes(trunkFlow);
        List<FlowNode> nodeOnlyTrunk = new ArrayList<>();
        List<FlowNode> nodeChanged = new ArrayList<>();
        List<FlowNode> nodeOnlyInBranch = new ArrayList<>();
        String nodeExceptions = "mqtt in;mqtt out;http in;http response;tcp in;tcp out;udp in;udp out;debug;";
        for (FlowNode lf : lvFlowNodes) {
        	boolean found = false;
        	for (FlowNode bf : brFlowNodes) {
				if (bf.getTabId().equalsIgnoreCase(lf.getTabId()) && bf.getNodeId().equalsIgnoreCase(lf.getNodeId())) {
					found = true;
					if (!(bf.getNodeContent().toString().equalsIgnoreCase(lf.getNodeContent().toString()))) {
						if (bf.getNodeContent().getString("type").equalsIgnoreCase("function")) {
							// Node is a function node, so determine the diff of the function content
							if (!bf.getNodeContent().getString("func").equalsIgnoreCase(lf.getNodeContent().getString("func"))) {
								// Node content is different
								DiffRowGenerator generator = DiffRowGenerator.create()
										.showInlineDiffs(true)
										.inlineDiffByWord(true)
										.oldTag(f -> f ? "<span class=\"del\">":"</span>")
										.newTag(f -> f ? "<span class=\"add\">":"</span>")
										.build();
								List<String> curfunc = Arrays.asList(bf.getNodeContent().getString("func").split("\n"));
								List<String> prvfunc = Arrays.asList(lf.getNodeContent().getString("func").split("\n"));
								List<DiffRow> rows = generator.generateDiffRows(prvfunc, curfunc);
								StringBuilder curhtml = new StringBuilder();
								StringBuilder prvhtml = new StringBuilder();
								for (DiffRow row : rows) {
									curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
									prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
								}
								if (!bf.getTabName().equalsIgnoreCase(lf.getTabName())) {
									bf.setTabName(bf.getTabName() + " (was: " + lf.getTabName() + ")");
								}
								if (!bf.getNodeName().equalsIgnoreCase(lf.getNodeName())) {
									bf.setNodeName(bf.getNodeName() + " (was: " + lf.getNodeName() + ")");
								}
								if (!bf.getTabName().equalsIgnoreCase(lf.getTabName())) {
									bf.setTabName(bf.getTabName() + " (was: " + lf.getTabName() + ")");
								}
								if (!bf.getNodeName().equalsIgnoreCase(lf.getNodeName())) {
									bf.setNodeName(bf.getNodeName() + " (was: " + lf.getNodeName() + ")");
								}
								FlowNode changed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "function code", curhtml.toString(), prvhtml.toString(), bf.getNodeContent());
								nodeChanged.add(changed);
							} else {
								// Function node content has not changed
								if (!bf.getTabName().equalsIgnoreCase(lf.getTabName())) {
									bf.setTabName(bf.getTabName() + " (was: " + lf.getTabName() + ")");
								}
								if (!bf.getNodeName().equalsIgnoreCase(lf.getNodeName())) {
									bf.setNodeName(bf.getNodeName() + " (was: " + lf.getNodeName() + ")");
								}
								String whatChanged = determineWhatChanged(lf.getNodeContent(), bf.getNodeContent());
								FlowNode changed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), whatChanged, bf.getNodeContent());
								nodeChanged.add(changed);
							}
						} else // Node is not a function node
						// Do not register the inputs and outputs changes
						if (!nodeExceptions.contains(bf.getNodeContent().getString("type"))) {
							if (!bf.getTabName().equalsIgnoreCase(lf.getTabName())) {
								bf.setTabName(bf.getTabName() + " (was: " + lf.getTabName() + ")");
							}
							if (!bf.getNodeName().equalsIgnoreCase(lf.getNodeName())) {
								bf.setNodeName(bf.getNodeName() + " (was: " + lf.getNodeName() + ")");
							}
							String whatChanged = determineWhatChanged(lf.getNodeContent(), bf.getNodeContent());
							FlowNode changed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), whatChanged, bf.getNodeContent());
							nodeChanged.add(changed);
						}
					}
				}
        	}
        	if (!found) {
        		// Node is only in live
        		if (lf.getNodeContent().getString("type").equalsIgnoreCase("function")) {
					FlowNode added = new FlowNode(lf.getTabId(), lf.getTabName(), lf.getNodeId(), lf.getNodeName(), "only in live", lf.getNodeContent().getString("func"), "", lf.getNodeContent());
					nodeOnlyTrunk.add(added);
        		} else {
					FlowNode added = new FlowNode(lf.getTabId(), lf.getTabName(), lf.getNodeId(), lf.getNodeName(), "only in live", lf.getNodeContent());
        			nodeOnlyTrunk.add(added);
        		}
        	}
        }
        for (FlowNode bf : brFlowNodes) {
        	boolean found = false;
        	for (FlowNode lf : lvFlowNodes) {
        		if (bf.getTabId().equalsIgnoreCase(lf.getTabId()) && bf.getNodeId().equalsIgnoreCase(lf.getNodeId())) {
        			found = true;
        		}
        	}
        	if (!found) {
        		// Node is only in branch
        		if (bf.getNodeContent().getString("type").equalsIgnoreCase("function")) {
					FlowNode removed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "only in branch", bf.getNodeContent().getString("func"), "", bf.getNodeContent());
					nodeOnlyInBranch.add(removed);
        		} else {
					FlowNode removed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "only in branch", bf.getNodeContent());
					nodeOnlyInBranch.add(removed);
        		}
        	}
        }
        Collections.sort(nodeOnlyTrunk);
        Collections.sort(nodeChanged);
        Collections.sort(nodeOnlyInBranch);
        String json = "{ \"added\":" + Utils.parser().toJson(nodeOnlyTrunk) + ",\"changed\":" + Utils.parser().toJson(nodeChanged) + ",\"removed\":" + Utils.parser().toJson(nodeOnlyInBranch) + "}";
        return json;
    }

	private List<FlowNode> getFlowNodes(String flow) {
		Map<String, String> tabs = new HashMap<>();
		List<FlowNode> nodes = new ArrayList<>();
		JsonArray jsonArray = Json.createReader(new StringReader(flow)).readArray();
		for (Object jo : jsonArray) {
			JsonObject obj = (JsonObject) jo;
			String type = obj.getString("type");
			switch (type) {
			case "tab":
				tabs.put(obj.getString("id"), obj.getString("label"));
				break;
			default:
				String zel = obj.getString("z");
				if (zel != null) {
					String parentId = zel.toString();
					nodes.add(new FlowNode(parentId, tabs.get(parentId), obj.getString("id"), obj.getString("name"), "",  obj));
				}
				break;
			}
		}
		return nodes;
	}

	private String determineWhatChanged(JsonObject live, JsonObject branch) {
		String change = "";
		try {
			JsonArray wl = live.get("wires").asJsonArray();
			JsonArray wb = branch.get("wires").asJsonArray();
			if (wl != wb) {
				change += "wiring";
			}
			int xl = live.getInt("x");
			int xb = branch.getInt("x");
			int yl = live.getInt("y");
			int yb = branch.getInt("y");
			if ((xl != xb) || (yl != yb)) {
				change += change == "" ? "" : " & " + "position";
			}
			if (change == "") {
				change = "property";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return change;
	}

	class Args {
		private String brName;
		private int revNo1;
		private int revNo2;

		public Args() {
		}

		public String getBrName () {
			return this.brName;
		}

		public void setBrName (String brName) {
			this.brName = brName;
		}

		public int getRevNo1 () {
			return this.revNo1;
		}

		public void setRevNo1 (int revNo1) {
			this.revNo1 = revNo1;
		}

		public int getRevNo2 () {
			return this.revNo2;
		}

		public void setRevNo2 (int revNo2) {
			this.revNo2 = revNo2;
		}


	}
}
