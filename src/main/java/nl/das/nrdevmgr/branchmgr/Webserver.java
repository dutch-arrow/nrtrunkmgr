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
 **  @svnversion     $Date: 2022-01-24 09:03:23 +0100 (Mon, 24 Jan 2022) $
 **                  $Revision: 50303 $
 **
 *******************************************************************************************
 */


package nl.das.nrdevmgr.branchmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.iki.elonen.router.RouterNanoHTTPD;
import nl.das.nrdevmgr.FlowNode;
import nl.das.nrdevmgr.SvnActions;
import nl.das.nrdevmgr.branchmgr.model.Node;

/**
 * @author tom
 *
 */
public class Webserver extends RouterNanoHTTPD {

	private static Properties props;
	private static SvnActions svn;
	private static String branchName;
	private static boolean uptodate = false;
	private static List<Node> mergedNodes = new ArrayList<>();

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
    	addRoute("/changednodes", GetNodes.class);
    	addRoute("/changeduis/:type", GetUI.class);
       	addRoute("/getbranch", GetBranch.class);
       	addRoute("/getbranches", GetBranches.class);
    	addRoute("/createbranch/:new", CreateBranch.class);
    	addRoute("/updatebranch", UpdateBranch.class);
    	addRoute("/removebranch", RemoveBranch.class);
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
				InputStream in = getClass().getResourceAsStream("/branchpage.html");
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

    public static class GetBranch extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
				CookieHandler cookies = session.getCookies();
				if (cookies != null) {
					props.setProperty("username",cookies.read("SvnUser"));
					props.setProperty("password",cookies.read("SvnPwd"));
				}
				svn = new SvnActions(props);
				branchName = svn.getBranch();
				if (branchName == "") {
					return newFixedLengthResponse("{\"branch\":\"\",\"msg\":\"No branch found in ~/.node-red\"}");
				}
				long[] revs = svn.getLatestTrunkRevisions();
				long hrev = getHighestRevision(revs);
				long rev = svn.getLatestTrunkRevInBranch(true, branchName);
				if (hrev <= rev) {
					uptodate = true;
					return newFixedLengthResponse("{\"branch\":\"" + branchName + "\",\"msg\":\"Branch is up-to-date\"}");
				}
				uptodate = false;
				return newFixedLengthResponse("{\"branch\":\"" + branchName  + "\", \"msg\":\"\"}");
			} catch (Exception e) {
				return newFixedLengthResponse("{\"branch\":\"\",\"msg\":\"" + e.getMessage() + "\"}");
			}
        }
    }

    public static class GetBranches extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
        		List<String> branches = svn.getMyBranches();
				return newFixedLengthResponse(parser().toJson(branches));
			} catch (Exception e) {
				return newFixedLengthResponse("{\"msg\":\"" + e.getMessage() + "\"}");
			}
        }
    }
    
    public static class GetNodes extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
        		// SVN trunk flows.json
				long[] revs = svn.getLatestTrunkRevisions();
				long hrev = getHighestRevision(revs);
				long rev = svn.getLatestTrunkRevInBranch(true, branchName);
				String json = "";
				if (hrev > rev) {
	        		String liveFlow = svn.getTrunkFlow(revs[0]);
	        		String branchFlow = svn.getBranchFlow(branchName, 0, true);
		            json = getDiffFlows(branchFlow, liveFlow);
				} else {
					json = "{\"msg\":\"Branch is up-to-date\"}";
				}
				return newFixedLengthResponse(json);
			} catch (Exception e) {
				return newFixedLengthResponse("{\"msg\":\"" + e.getMessage() + "\"}");
			}
        }
    }

    public static class GetUI extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
          		String type = urlParams.get("type");
				long[] revs = svn.getLatestTrunkRevisions();
				long hrev = getHighestRevision(revs);
				long rev = svn.getLatestTrunkRevInBranch(true, branchName);
				String json = "";
				if (hrev > rev) {
					switch (type) {
					case "html": {
			            String html1 = svn.getBranchUi("html", branchName, 0, true);
			            String html2 = svn.getTrunkUi("html", revs[1]);
			            json = getDiffHtml(html1, html2);
			            break;
					}
					case "js": {
			            String html1 = svn.getBranchUi("js", branchName, 0, true);
			            String html2 = svn.getTrunkUi("js", revs[2]);
			            json = getDiffJs(html1, html2);
			            break;
					}
					case "css": {
			            String html1 = svn.getBranchUi("css", branchName, 0, true);
			            String html2 = svn.getTrunkUi("css", revs[3]);
			            json = getDiffCss(html1, html2);
			            break;
					}
					}
				} else {
					json = "{\"msg\":\"Branch is up-to-date\"}";
				}
				return newFixedLengthResponse(json);
			} catch (Exception e) {
				return newFixedLengthResponse("{\"msg\":\"getUI-" + e.getMessage() + "\"}");
			}
        }
    }

    public static class CreateBranch extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
          		String name = urlParams.get("new");
          		String parts[] = name.split(":");
        		svn.createBranch(parts[0]);
          		if (parts.length == 2) {
          			svn.removeBranch(parts[1]);
          		}
				return newFixedLengthResponse("");
			} catch (Exception e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }
    

    public static class RemoveBranch extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
        	try {
            	final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
	            String requestBody = map.get("postData");
	    		JsonArray jsonArray = new JsonParser().parse(requestBody).getAsJsonArray();
	    		for (JsonElement jo : jsonArray) {
    				String nm = jo.getAsString();
    				svn.removeBranch(nm);
	    		}
				return newFixedLengthResponse("");
			} catch (Exception e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }
    
    public static class UpdateBranch extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
	       	try {
	           	if (!uptodate) {
	            	final HashMap<String, String> map = new HashMap<String, String>();
					session.parseBody(map);
		            String requestBody = map.get("postData");
		    		JsonObject jsonObject = new JsonParser().parse(requestBody).getAsJsonObject();
		    		// Nodes
		    		JsonArray jsonArray = jsonObject.getAsJsonArray("nodes");
		    		for (JsonElement jo : jsonArray) {
	    				JsonObject obj = (JsonObject) jo;
	    				String type = obj.get("nodeContent").getAsJsonObject().get("type").getAsString();
	    				mergedNodes.add(new Node(obj.get("nodeId").getAsString(), type, obj.get("nodeContent").getAsJsonObject()));
		    		}
	        		// Do final merge
	        		JsonArray newFlows = new JsonArray();
	        		List<Node> newNodes = new ArrayList<>();
	        		newNodes.addAll(mergedNodes);
	        		for (Node n : newNodes) {
	        			newFlows.add(n.getJson());
	        		}
	        		String json = parser().toJson(newFlows);
	        		svn.updateFlow(json);
	        		// HTML
	        		String ui = jsonObject.get("html").getAsString();
	        		if (ui != "") {
	        			svn.updateUi("html", ui);
	        		}
	        		// JS
	        		ui = jsonObject.get("js").getAsString();
	        		if (ui != "") {
	        			svn.updateUi("js", ui);
	        		}
	        		// CSS
	        		ui = jsonObject.get("css").getAsString();
	        		if (ui != "") {
	        			svn.updateUi("css", ui);
	        		}
	        		// Commit branch
	        		svn.commit("Merged");
	    			// Register branch as been merged
	    			svn.merge(true);
	        	}
				return newFixedLengthResponse("");
			} catch (Exception e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    private static String getDiffFlows(String branchFlow, String liveFlow) throws UnsupportedEncodingException {
        try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("src/test/resources/flows_branch.json"));
			writer.write(branchFlow);
			writer.close();
			writer = new BufferedWriter(new FileWriter("src/test/resources/flows_live.json"));
			writer.write(liveFlow);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        mergedNodes = new ArrayList<>();
        List<FlowNode> brFlowNodes = getFlowNodes(branchFlow);
        List<FlowNode> lvFlowNodes = getFlowNodes(liveFlow);
        getNonFlowNodes(branchFlow);
        getNonFlowNodes(liveFlow);
        List<FlowNode> nodeOnlyInLive = new ArrayList<>();
        List<FlowNode> nodeChanged = new ArrayList<>();
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
										.oldTag(f -> f ? "<span class=\"add\">":"</span>")
										.newTag(f -> f ? "<span class=\"del\">":"</span>")
										.build();
								List<String> curfunc = Arrays.asList(bf.getNodeContent().get("func").getAsString().split("\n"));
								List<String> prvfunc = Arrays.asList(lf.getNodeContent().get("func").getAsString().split("\n"));
								List<DiffRow> rows = generator.generateDiffRows(prvfunc, curfunc);
								StringBuilder curhtml = new StringBuilder();
								StringBuilder prvhtml = new StringBuilder();
								prvhtml.append("<div contenteditable=\"false\">");
								for (DiffRow row : rows) {
									curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
									prvhtml.append(row.getOldLine() + "<br/>");
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
								prvhtml.append("</div>");
								FlowNode changed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "function code", curhtml.toString(), prvhtml.toString(), bf.getNodeContent());
								nodeChanged.add(changed);
							}
						} else // Node is not a function node
						if (!nodeExists(bf.getNodeId())) {
							mergedNodes.add(new Node(bf.getNodeId(),bf.getNodeContent().get("type").getAsString(),bf.getNodeContent()));
//							System.out.println("other nodes:  " + bf.getNodeId() + " " + bf.getNodeName() + " " + bf.getNodeContent().get("type").getAsString());
						}
					} else // Node is the same, so add them as merged
					if (!nodeExists(bf.getNodeId())) {
						mergedNodes.add(new Node(bf.getNodeId(), bf.getNodeContent().get("type").getAsString(), bf.getNodeContent()));
//						System.out.println("equal nodes:  " + bf.getNodeId() + " " + bf.getNodeName() + " " + bf.getNodeContent().get("type").getAsString());
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
					break;
				}
        	}
        	// Node only in branch, so keep them
			if (!found && !nodeExists(bf.getNodeId())) {
				mergedNodes.add(new Node(bf.getNodeId(), bf.getNodeContent().get("type").getAsString(), bf.getNodeContent()));
//				System.out.println("only in branch:  " + bf.getNodeId() + " " + bf.getNodeName() + " " + bf.getNodeContent().get("type").getAsString());
			}
        }
        Collections.sort(nodeOnlyInLive);
        Collections.sort(nodeChanged);
        String json = "{ \"added\":" + parser().toJson(nodeOnlyInLive) + ",\"changed\":" + parser().toJson(nodeChanged) + "}";
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

	private static void getNonFlowNodes(String flow) {
		JsonArray jsonArray = new JsonParser().parse(flow).getAsJsonArray();
		for (JsonElement jo : jsonArray) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject obj = (JsonObject) jo;
				String type = obj.get("type").getAsString();
				JsonElement zel = obj.get("z");
				if ((zel == null) && !nodeExists(obj.get("id").getAsString())) {
					mergedNodes.add(new Node(obj.get("id").getAsString(), type,  obj));
				}
			}
		}
	}

	private static boolean nodeExists(String id) {
		boolean found = false;
		for (Node n : mergedNodes) {
			if (n.getId().equalsIgnoreCase(id)) {
				found = true;
				break;
			}
		}
		return found;
	}

    private static String getDiffHtml(String curHtml, String prvHtml) throws SVNException, UnsupportedEncodingException {
    	if (curHtml.equals(prvHtml)) {
    		return "[\"\",\"\"]";
    	}
		Gson gson = new Gson();
		DiffRowGenerator generator = DiffRowGenerator.create()
				.showInlineDiffs(true)
				.inlineDiffByWord(true)
				.oldTag(f -> f ? "<span class=\"add\">":"</span>")
				.newTag(f -> f ? "<span class=\"del\">":"</span>")
				.build();
		String cur = curHtml.replace("<", "&lt;").replace(">", "&gt;");
		String prv = prvHtml.replace("<", "&lt;").replace(">", "&gt;");
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		prvhtml.append("<div contenteditable=\"false\">");
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
			prvhtml.append(row.getOldLine() + "<br/>");
		}
		prvhtml.append("</div>");
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
				.oldTag(f -> f ? "<span class=\"add\">":"</span>")
				.newTag(f -> f ? "<span class=\"del\">":"</span>")
				.build();
		String cur = curJs;
		String prv = prvJs;
		StringBuilder curhtml = new StringBuilder();
		StringBuilder prvhtml = new StringBuilder();
		List<String> curlns = Arrays.asList(cur.split("\n"));
		List<String> prvlns = Arrays.asList(prv.split("\n"));
		List<DiffRow> rows = generator.generateDiffRows(prvlns, curlns);
		for (DiffRow row : rows) {
			curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
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
			curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
			prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
		}
		String json = "[" + gson.toJson(curhtml.toString()) + "," + gson.toJson(prvhtml.toString()) + "]";
		return json;
    }
	
	public static long getHighestRevision(long[] revs) {
	    return Arrays.stream(revs).max().getAsLong();

	}

	public static Gson parser() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson;
	}
}

