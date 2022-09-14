/**
 *******************************************************************************************
 **
 **  @filename       Webserver.java
 **  @brief
 **
 **  @copyright      (c) Core|Vision B.V.,
 **                  Cereslaan 10b,
 **                  5384 VT  Heesch,
 **                  The Netherlands,
 **                  All Rights Reserved
 **
 **  @author         tom
 **  @svnversion     $Date: 2022-03-14 17:43:02 +0100 (Mon, 14 Mar 2022) $
 **                  $Revision: 50895 $
 **
 *******************************************************************************************
 */


package nl.das.nrdevmgr.anydbmgr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.tmatesoft.svn.core.SVNException;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.iki.elonen.router.RouterNanoHTTPD;
import nl.das.nrdevmgr.FlowNode;
import nl.das.nrdevmgr.SvnActions;

/**
 * @author tom
 *
 */
public class Webserver extends RouterNanoHTTPD {

	private static Properties props;
	private static SvnActions svn;
	private static String branchName;
	private static boolean merge;
	private static long[] brevs;

	public Webserver(Properties properties) throws IOException, SVNException {
        super(properties.getProperty("host"), Integer.parseInt(properties.getProperty("port")));
        props = properties;
        addMappings();
	}

	/*
	 * Methods
	 */

    @Override
    public void addMappings() {
		setNotFoundHandler(Error404UriHandler.class);
		setNotImplementedHandler(NotImplementedHandler.class);
    	addRoute("/branches", GetBranches.class);
    	addRoute("/revisions/:branch", GetRevisions.class);
    	addRoute("/sqlrevisions", GetSqlRevisions.class);
    	addRoute("/flows", GetFlows.class);
    	addRoute("/uis/:type", GetUis.class);
    	addRoute("/sqls/:type", GetSqls.class);
    	addRoute("/changedflows", GetChangedFlows.class);
    	addRoute("/changeduis", GetChangedUis.class);
     	addRoute("/devliveflows", GetBranchTrunkFlows.class);
    	addRoute("/devliveuis/:type", GetBranchTrunkUis.class);
    	addRoute("/commit/:msg", CommitFlows.class);
    	addRoute("/mergecommit/:msg", MergeCommitFlows.class);
    	addRoute("/favicon.ico", GetIcon.class);
    	addRoute("/", GetPage.class);
    }
    /*
     * Handlers
     */
    public static class GetIcon extends GeneralHandler {

        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
			InputStream in = getClass().getResourceAsStream("/favicon.ico");
			return newChunkedResponse(Response.Status.OK, "image/icon", in);
		}
    }
    
    public static class GetPage extends GeneralHandler {

        @Override
        public Response get(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
			try {
				InputStream in = getClass().getResourceAsStream("/trunkpage.html");
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			    char[] arr = new char[8 * 1024];
			    StringBuilder buffer = new StringBuilder();
			    int numCharsRead;
			    while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
			        buffer.append(arr, 0, numCharsRead);
			    }
			    reader.close();
			    String page = buffer.toString();
				return newFixedLengthResponse(page);
			} catch (IOException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetBranches extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
				CookieHandler cookies = session.getCookies();
				if (cookies != null) {
					props.setProperty("username",cookies.read("SvnUser"));
					props.setProperty("password",cookies.read("SvnPwd"));
				}
				svn = new SvnActions(props);
				List<String> branches = svn.getAllBranches();
		        int isDirty = svn.isTrunkDirty();
				Gson gson = new Gson();
				String bjson = gson.toJson(branches);
				String json = "{\"hasChanges\":" + isDirty + ",\"curLiveRev\":" + svn.getLatestTrunkRevision() + ",\"branches\":" + bjson + "}";
				return newFixedLengthResponse(json);
			} catch (SVNException | IOException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetRevisions extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
        		//
           		branchName = urlParams.get("branch");
           		RevisionData rd = new RevisionData();
           		merge = true;
				long[] revs = svn.getLatestTrunkRevisions();
				brevs = svn.getLatestBranchRevisions();
				long hrev = getHighestRevision(revs);
				long rev = svn.getLatestTrunkRevInBranch(false, branchName);
				if (hrev > rev) {
        			merge = false;
        			rd.setMessage("The branch is not up-to-date with latest trunk");
				}
				rd.setLiveFlows(svn.getAllRevisionNumbers("trunk", 'f'));
				rd.setLiveUiHtml(svn.getAllRevisionNumbers("trunk", 'h'));
				rd.setLiveUiJs(svn.getAllRevisionNumbers("trunk", 'j'));
				rd.setLiveUiCss(svn.getAllRevisionNumbers("trunk", 'c'));
				rd.setDevliveFlows(Arrays.asList(svn.getLatestBranchRevisions()[0], revs[0]));
				rd.setDevliveUiHtml(Arrays.asList(svn.getLatestBranchRevisions()[1], revs[1]));
				rd.setDevliveUiJs(Arrays.asList(svn.getLatestBranchRevisions()[2], revs[2]));
				rd.setDevliveUiCss(Arrays.asList(svn.getLatestBranchRevisions()[3], revs[3]));
				Gson gson = new Gson();
				String json = gson.toJson(rd);
	            return newFixedLengthResponse(json);
			} catch (SVNException | UnsupportedEncodingException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetSqlRevisions extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
           		RevisionData rd = new RevisionData();
				rd.setTableSql(svn.getSqlRevisions('t'));
				rd.setViewSql(svn.getSqlRevisions('v'));
				rd.setProcSql(svn.getSqlRevisions('p'));
				Gson gson = new Gson();
				String json = gson.toJson(rd);
	            return newFixedLengthResponse(json);
			} catch (SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetFlows extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
            	final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	            Gson gson = new Gson();
	            SvnRevisions revs = gson.fromJson(requestBody, SvnRevisions.class);
	            String flow1 = svn.getTrunkFlow(revs.getRevNo1());
	            String flow2 = svn.getTrunkFlow(revs.getRevNo2());
	            StringBuilder json = new StringBuilder(getDiffFlows(flow1, flow2));
	            return newFixedLengthResponse(json.toString());
			} catch (IOException | ResponseException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetUis extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
          		String type = urlParams.get("type");
          		final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	            Gson gson = new Gson();
	            SvnRevisions revs = gson.fromJson(requestBody, SvnRevisions.class);
	            String r1,r2,json = "";
	            r1 = svn.getTrunkUi(type, revs.getRevNo1());
	            r2 = svn.getTrunkUi(type, revs.getRevNo2());
	            switch (type) {
	            case "html":
		            json = getDiffHtml(r1, r2);
		            break;
	            case "js":
		            json = getDiffJs(r1, r2);
		            break;
	            case "css":
		            json = getDiffCss(r1, r2);
		            break;
	            }
	            return newFixedLengthResponse(json);
			} catch (IOException | ResponseException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetSqls extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
           		String type = urlParams.get("type");
            	final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	            Gson gson = new Gson();
	            SvnRevisions revs = gson.fromJson(requestBody, SvnRevisions.class);
	            String r1,r2,json = "";
	            r1 = svn.getTrunkSql(revs.getRevNo1(), type);
	            r2 = svn.getTrunkSql(revs.getRevNo2(), type);
	            switch (type) {
	            case "tables":
		            json = getDiffTables(r1, r2);
	            	break;
	            case "views":
		            json = getDiffViews(r1, r2);
	            	break;
	            case "procedures":
		            json = getDiffProcs(r1, r2);
	            	break;
	            }
	            return newFixedLengthResponse(json);
			} catch (IOException | ResponseException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetChangedFlows extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
	            String flow1 = svn.getTrunkFlow(0);
	            String flow2 = svn.getTrunkFlow(-1);
	            StringBuilder json = new StringBuilder(getDiffFlows(flow1, flow2));
	            return newFixedLengthResponse(json.toString());
			} catch (IOException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetChangedUis extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
	            String html1 = svn.getTrunkUi("html", 0);
	            String html2 = svn.getTrunkUi("html", -1);
	            String jsonHtml = getDiffHtml(html1, html2);
	            html1 = svn.getTrunkUi("js", 0);
	            html2 = svn.getTrunkUi("js", -1);
	            String jsonJs = getDiffJs(html1, html2);
	            html1 = svn.getTrunkUi("css", 0);
	            html2 = svn.getTrunkUi("css", -1);
	            String jsonCss = getDiffCss(html1, html2);
	            String json = "{\"html\":" + jsonHtml + ",\"js\":" + jsonJs + ",\"css\":" + jsonCss + "}";
	            return newFixedLengthResponse(json);
			} catch (IOException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetBranchTrunkFlows extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
            	final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	            Gson gson = new Gson();
	            SvnRevisions revs = gson.fromJson(requestBody, SvnRevisions.class);
	            String flow1 = svn.getBranchFlow(branchName, revs.getRevNo1(), false);
	            String flow2 = svn.getTrunkFlow(revs.getRevNo2());
	            String json = getDiffFlows(flow1, flow2);
	            return newFixedLengthResponse(json);
			} catch (IOException | ResponseException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
            }
        }

    }

    public static class GetBranchTrunkUis extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
           		String type = urlParams.get("type");
            	final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	            Gson gson = new Gson();
	            SvnRevisions revs = gson.fromJson(requestBody, SvnRevisions.class);
	            String r1,r2,json = "";
	            switch (type) {
	            case "html":
		            r1 = svn.getBranchUi("html", branchName, revs.getRevNo1(), false);
		            r2 = svn.getTrunkUi("html", revs.getRevNo2());
		            json = getDiffHtml(r1, r2);
	            	break;
	            case "js":
		            r1 = svn.getBranchUi("js", branchName, revs.getRevNo1(), false);
		            r2 = svn.getTrunkUi("js", revs.getRevNo2());
		            json = getDiffJs(r1, r2);
	            	break;
	            case "css":
		            r1 = svn.getBranchUi("css", branchName, revs.getRevNo1(), false);
		            r2 = svn.getTrunkUi("css", revs.getRevNo2());
		            json = getDiffCss(r1, r2);
	            	break;
	            }
	            return newFixedLengthResponse(json);
			} catch (IOException | ResponseException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }

    }

    public static class CommitFlows extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
        		String msg = urlParams.get("msg");
        		if ((msg == null) || (msg.length() == 0)) {
        			msg = "No message given";
        		}
        		String json = svn.commit(msg) + "";
	            return newFixedLengthResponse(json);
			} catch (SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class MergeCommitFlows extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
        		String msg = urlParams.get("msg");
        		if ((msg == null) || (msg.length() == 0)) {
        			msg = "No message given";
        		}
        		if (!merge) {
        			return newFixedLengthResponse("Cannot merge&commit: The branch is not up-to-date with latest trunk");
        		}
            	final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	            Gson gson = new Gson();
	            SvnRevisions revs = gson.fromJson(requestBody, SvnRevisions.class);
        		if ((brevs[0] == revs.getRevNo1()) && (brevs[1] == revs.getRevNo2()) && (brevs[2] == revs.getRevNo3()) && (brevs[3] == revs.getRevNo4())) {
        			return newFixedLengthResponse("Branch content hasn't changed, so no merge done.");
        		}
        		// Get the changed files from the "dev" repo and write them into the "live" repo workdir
	            String devflow = svn.getBranchFlow(branchName, revs.getRevNo1(), false);
	            String devhtml = svn.getBranchUi("html", branchName, revs.getRevNo2(), false);
	            String devjs = svn.getBranchUi("js", branchName, revs.getRevNo3(), false);
	            String devcss = svn.getBranchUi("css", branchName, revs.getRevNo4(), false);
	            if (brevs[0] != revs.getRevNo1()) {
	            	svn.updateFlow(devflow);
	            }
	            if (brevs[1] != revs.getRevNo2()) {
	            	svn.updateUi("html", devhtml);
	            }
	            if (brevs[2] != revs.getRevNo3()) {
	            	svn.updateUi("js", devjs);
	            }
	            if (brevs[3] != revs.getRevNo4()) {
	            	svn.updateUi("css", devcss);
	            }
	            System.out.println("updated");
	            // Then commit the "live" repo
        		String json = svn.commit(msg) + "";
	            System.out.println("committed");
        		// Register the merge
        		svn.merge(false);
	            System.out.println("merged");
        		return newFixedLengthResponse(json);
			} catch (SVNException | IOException | ResponseException  e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }
    private static String getDiffFlows(String branchFlow, String liveFlow) throws UnsupportedEncodingException {

        Gson gson = new Gson();
        List<FlowNode> brFlowNodes = getFlowNodes(branchFlow);
        List<FlowNode> lvFlowNodes = getFlowNodes(liveFlow);
        List<FlowNode> nodeOnlyInLive = new ArrayList<>();
        List<FlowNode> nodeChanged = new ArrayList<>();
        List<FlowNode> nodeOnlyInBranch = new ArrayList<>();
        String nodeExceptions = "mqtt in;mqtt out;http in;http response;tcp in;tcp out;udp in;udp out;debug;";
        for (FlowNode lf : lvFlowNodes) {
        	boolean found = false;
        	for (FlowNode bf : brFlowNodes) {
				if (bf.getTabId().equalsIgnoreCase(lf.getTabId()) && bf.getNodeId().equalsIgnoreCase(lf.getNodeId())) {
					found = true;
					if (!(bf.getNodeContent().toString().equalsIgnoreCase(lf.getNodeContent().toString()))) {
						if (bf.getNodeContent().get("type").getAsString().equalsIgnoreCase("function")) {
							// Node is a function node, so determine the diff of the function content
							if (!bf.getNodeContent().get("func").getAsString().equalsIgnoreCase(lf.getNodeContent().get("func").getAsString())) {
								// Node content is different
								DiffRowGenerator generator = DiffRowGenerator.create()
										.showInlineDiffs(true)
										.inlineDiffByWord(true)
										.oldTag(f -> f ? "<span class=\"del\">":"</span>")
										.newTag(f -> f ? "<span class=\"add\">":"</span>")
										.build();
								List<String> curfunc = Arrays.asList(bf.getNodeContent().get("func").getAsString().split("\n"));
								List<String> prvfunc = Arrays.asList(lf.getNodeContent().get("func").getAsString().split("\n"));
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
						if (!nodeExceptions.contains(bf.getNodeContent().get("type").getAsString())) {
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
        		if (lf.getNodeContent().get("type").getAsString().equalsIgnoreCase("function")) {
					FlowNode added = new FlowNode(lf.getTabId(), lf.getTabName(), lf.getNodeId(), lf.getNodeName(), "only in live", lf.getNodeContent().get("func").getAsString(), "", lf.getNodeContent());
					nodeOnlyInLive.add(added);
        		} else {
					FlowNode added = new FlowNode(lf.getTabId(), lf.getTabName(), lf.getNodeId(), lf.getNodeName(), "only in live", lf.getNodeContent());
        			nodeOnlyInLive.add(added);
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
        		if (bf.getNodeContent().get("type").getAsString().equalsIgnoreCase("function")) {
					FlowNode removed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "only in branch", bf.getNodeContent().get("func").getAsString(), "", bf.getNodeContent());
					nodeOnlyInBranch.add(removed);
        		} else {
					FlowNode removed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "only in branch", bf.getNodeContent());
					nodeOnlyInBranch.add(removed);
        		}
        	}
        }
        Collections.sort(nodeOnlyInLive);
        Collections.sort(nodeChanged);
        Collections.sort(nodeOnlyInBranch);
        String json = "{ \"added\":" + gson.toJson(nodeOnlyInLive) + ",\"changed\":" + gson.toJson(nodeChanged) + ",\"removed\":" + gson.toJson(nodeOnlyInBranch) + "}";
        return json;
    }

	private static List<FlowNode> getFlowNodes(String flow) {
		Map<String, String> tabs = new HashMap<>();
		List<FlowNode> nodes = new ArrayList<>();
		JsonArray jsonArray = new JsonParser().parse(flow).getAsJsonArray();
		for (JsonElement jo : jsonArray) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject obj = (JsonObject) jo;
				String type = obj.get("type").getAsString();
				switch (type) {
				case "tab":
					tabs.put(obj.get("id").getAsString(), obj.get("label").getAsString());
					break;
				default:
					JsonElement zel = obj.get("z");
					if (zel != null) {
						String parentId = zel.getAsString();
						nodes.add(new FlowNode(parentId, tabs.get(parentId), obj.get("id").getAsString(), obj.get("name").getAsString(), "",  obj));
					}
					break;
				}
			}
		}
		return nodes;
	}

	private static String determineWhatChanged(JsonObject live, JsonObject branch) {
		String change = "";
		try {
			JsonArray wl = live.get("wires").getAsJsonArray();
			JsonArray wb = branch.get("wires").getAsJsonArray();
			if (wl != wb) {
				change += "wiring";
			}
			int xl = live.get("x").getAsInt();
			int xb = branch.get("x").getAsInt();
			int yl = live.get("y").getAsInt();
			int yb = branch.get("y").getAsInt();
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

    private static String getDiffHtml(String curHtml, String prvHtml) throws SVNException, UnsupportedEncodingException {
    	if (curHtml.equals(prvHtml)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"del\">":"</span>")
				.newTag(f -> f ? "<span class=\"add\">":"</span>")
				.build();
		String cur = curHtml.replace("<", "&lt;").replace(">", "&gt;");
		String prv = prvHtml.replace("<", "&lt;").replace(">", "&gt;");
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffJs(String curJs, String prvJs) throws SVNException, UnsupportedEncodingException {
    	if (curJs.equals(prvJs)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"del\">":"</span>")
				.newTag(f -> f ? "<span class=\"add\">":"</span>")
				.build();
		String cur = curJs;
		String prv = prvJs;
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffCss(String curCss, String prvCss) throws SVNException, UnsupportedEncodingException {
    	if (curCss.equals(prvCss)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"del\">":"</span>")
				.newTag(f -> f ? "<span class=\"add\">":"</span>")
				.build();
		String cur = curCss;
		String prv = prvCss;
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffTables(String current, String previous) throws SVNException, UnsupportedEncodingException {
    	if (current.equals(previous)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"del\">":"</span>")
				.newTag(f -> f ? "<span class=\"add\">":"</span>")
				.build();
		String cur = current;
		String prv = previous;
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffViews(String current, String previous) throws SVNException, UnsupportedEncodingException {
    	if (current.equals(previous)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"del\">":"</span>")
				.newTag(f -> f ? "<span class=\"add\">":"</span>")
				.build();
		String cur = current;
		String prv = previous;
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffProcs(String current, String previous) throws SVNException, UnsupportedEncodingException {
    	if (current.equals(previous)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"del\">":"</span>")
				.newTag(f -> f ? "<span class=\"add\">":"</span>")
				.build();
		String cur = current;
		String prv = previous;
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }
	
	private static long getHighestRevision(long[] revs) {
	    return Arrays.stream(revs).max().getAsLong();
	}
}

