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


package nl.das.nrdevmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.tmatesoft.svn.core.SVNException;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;

import fi.iki.elonen.router.RouterNanoHTTPD;
import nl.das.nrdevmgr.model.FlowNode;
import nl.das.nrdevmgr.model.Node;
import nl.das.nrdevmgr.model.RevisionData;

/**
 * @author tom
 *
 */
public class Webserver extends RouterNanoHTTPD {

	private static Properties props;
	private static boolean trunk;
	private static SvnActions svn;
	private static String branchName;
	private static boolean uptodate = false;
	private static boolean merge;
	private static long[] brevs;
	private static List<Node> mergedNodes = new ArrayList<>();

	public Webserver(Properties properties, String tbtype) throws IOException, SVNException {
        super(properties.getProperty("host"), Integer.parseInt(properties.getProperty("port")));
        props = properties;
        trunk = tbtype.equalsIgnoreCase("trunk");
        addMappings();
	}

	/*
	 * Methods
	 */

    @Override
    public void addMappings() {
		setNotFoundHandler(Error404UriHandler.class);
		setNotImplementedHandler(NotImplementedHandler.class);
		// trunk manager URLs
    	addRoute("/branches", GetAllBranches.class);
    	addRoute("/revisions/:branch", GetRevisions.class);
    	addRoute("/sqlrevisions", GetSqlRevisions.class);
    	addRoute("/flows", GetFlows.class);
    	addRoute("/uis/:type", GetUis.class);
    	addRoute("/sqls/:type", GetSqls.class);
    	addRoute("/changedflows", GetChangedTrunkFlows.class);
    	addRoute("/changeduis", GetChangedTrunkUis.class);
     	addRoute("/branchtrunkflows", GetBranchTrunkFlows.class);
    	addRoute("/branchtrunkuis/:type", GetBranchTrunkUis.class);
    	addRoute("/commit/:msg", CommitFlows.class);
    	addRoute("/mergecommit/:msg", MergeCommitFlows.class);
    	// branch manager URLs
    	addRoute("/changednodes", GetNodes.class);
    	addRoute("/changeduis/:type", GetUI.class);
       	addRoute("/getbranch", GetBranch.class);
       	addRoute("/getbranches", GetBranches.class);
    	addRoute("/createbranch/:new", CreateBranch.class);
    	addRoute("/updatebranch", UpdateBranch.class);
    	addRoute("/removebranch", RemoveBranch.class);
    	// common URLs
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
				InputStream in = getClass().getResourceAsStream(trunk ? "/trunkpage.html" : "/branchpage.html");
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			    StringBuilder buffer = new StringBuilder();
				String line = reader.readLine();
				while (line != null) {
					if (line.contains("<include>")) {
						String prefix = line.substring(0, line.indexOf("<include>"));
						String postfix = line.substring(line.indexOf("</include>") + 10);
						String incfile = line.substring(line.indexOf("<include>") + 9 , line.indexOf("</include>")).trim();
						buffer.append(prefix);
						buffer.append(readAndSubstitute(incfile));
						buffer.append(postfix + "\n");
					} else {
						buffer.append(line + "\n");
					}
					line = reader.readLine();
				}
			    reader.close();
			    String page = buffer.toString();
				return newFixedLengthResponse(page);
			} catch (IOException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }

        private String readAndSubstitute(String path) throws IOException {
			InputStream in = getClass().getResourceAsStream(path);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		    StringBuilder buffer = new StringBuilder();
			String line = reader.readLine();
			while (line != null) {
				if (line.contains("<include>")) {
					String prefix = line.substring(0, line.indexOf("<include>"));
					String postfix = line.substring(line.indexOf("</include>") + 10);
					String incfile = line.substring(line.indexOf("<include>") + 9, line.indexOf("</include>")).trim();
					buffer.append(prefix);
					buffer.append(readAndSubstitute(incfile));
					buffer.append(postfix + "\n");

				} else {
					buffer.append(line + "\n");
				}
				line = reader.readLine();
			}
		    reader.close();
		    return buffer.toString();
        }
    }

    public static class GetAllBranches extends GeneralHandler {

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
		        int isDirty = svn.isWCDirty();
				String bjson = parser().toJson(branches);
				String json = "{\"hasChanges\":" + isDirty + ",\"curTrunkRev\":" + svn.getLatestTrunkRevision() + ",\"branches\":" + bjson + "}";
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
				brevs = svn.getLatestBranchRevisions(branchName);
				long hrev = getHighestRevision(revs);
				long rev = svn.getLatestTrunkRevInBranch(false, branchName);
				if (hrev > rev) {
        			merge = false;
        			rd.setMessage("The branch is not up-to-date with latest trunk");
				}
				rd.setTrunkFlows(svn.getAllRevisionNumbers("trunk", 'f'));
				rd.setTrunkUiHtml(svn.getAllRevisionNumbers("trunk", 'h'));
				rd.setTrunkUiJs(svn.getAllRevisionNumbers("trunk", 'j'));
				rd.setTrunkUiCss(svn.getAllRevisionNumbers("trunk", 'c'));
				rd.setBranchtrunkFlows(Arrays.asList(svn.getLatestBranchRevisions(branchName)[0], revs[0]));
				rd.setBranchtrunkUiHtml(Arrays.asList(svn.getLatestBranchRevisions(branchName)[1], revs[1]));
				rd.setBranchtrunkUiJs(Arrays.asList(svn.getLatestBranchRevisions(branchName)[2], revs[2]));
				rd.setBranchtrunkUiCss(Arrays.asList(svn.getLatestBranchRevisions(branchName)[3], revs[3]));
				String json = parser().toJson(rd);
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
				String json = parser().toJson(rd);
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
	    		JsonObject obj = Json.createReader(new StringReader(requestBody)).readObject();
	            String flow1 = svn.getTrunkFlow(obj.getInt("revNo1"));
	            String flow2 = svn.getTrunkFlow(obj.getInt("revNo2"));
	            StringBuilder json = new StringBuilder(getDiffTrunkFlows(flow1, flow2));
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
	    		JsonObject obj = Json.createReader(new StringReader(requestBody)).readObject();
	            String r1,r2,json = "";
	            r1 = svn.getTrunkUi(type, obj.getInt("revNo1"));
	            r2 = svn.getTrunkUi(type, obj.getInt("revNo2"));
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
	    		JsonObject obj = Json.createReader(new StringReader(requestBody)).readObject();
	            String r1,r2,json = "";
	            r1 = svn.getTrunkSql(obj.getInt("revNo1"), type);
	            r2 = svn.getTrunkSql(obj.getInt("revNo2"), type);
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

    public static class GetChangedTrunkFlows extends GeneralHandler {

        @Override
        public Response post(UriResource uriResource, Map<String, String> urlParams, IHTTPSession session) {
            try {
	            String flow1 = svn.getTrunkFlow(0); // from WC
	            String flow2 = svn.getTrunkFlow(-1);// HEAD rev from repo
	            StringBuilder json = new StringBuilder(getDiffTrunkFlows(flow1, flow2));
	            return newFixedLengthResponse(json.toString());
			} catch (IOException | SVNException e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    public static class GetChangedTrunkUis extends GeneralHandler {

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
	    		JsonObject obj = Json.createReader(new StringReader(requestBody)).readObject();
	            String bflow = svn.getBranchFlow(branchName, obj.getInt("revNo1"), false);
	            String tflow = svn.getTrunkFlow(obj.getInt("revNo2"));
	            String json = getDiffTrunkFlows(bflow, tflow);
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
	    		JsonObject obj = Json.createReader(new StringReader(requestBody)).readObject();
	            String r1,r2,json = "";
	            switch (type) {
	            case "html":
		            r1 = svn.getBranchUi("html", branchName,obj.getInt("revNo1"), false);
		            r2 = svn.getTrunkUi("html", obj.getInt("revNo2"));
		            json = getDiffHtml(r1, r2);
	            	break;
	            case "js":
		            r1 = svn.getBranchUi("js", branchName, obj.getInt("revNo1"), false);
		            r2 = svn.getTrunkUi("js", obj.getInt("revNo2"));
		            json = getDiffJs(r1, r2);
	            	break;
	            case "css":
		            r1 = svn.getBranchUi("css", branchName, obj.getInt("revNo1"), false);
		            r2 = svn.getTrunkUi("css", obj.getInt("revNo2"));
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
	    		JsonObject obj = Json.createReader(new StringReader(requestBody)).readObject();
//        		if ((brevs[0] == obj.getInt("revNo1")) && (brevs[1] == obj.getInt("revNo2")) && (brevs[2] == obj.getInt("revNo3")) && (brevs[3] == obj.getInt("revNo4"))) {
//        			return newFixedLengthResponse("Branch content hasn't changed, so no merge done.");
//        		}
        		// Get the changed files from the "dev" repo and write them into the "live" repo workdir
	            String devflow = svn.getBranchFlow(branchName, obj.getInt("revNo1"), false);
	            String devhtml = svn.getBranchUi("html", branchName, obj.getInt("revNo2"), false);
	            String devjs = svn.getBranchUi("js", branchName, obj.getInt("revNo3"), false);
	            String devcss = svn.getBranchUi("css", branchName, obj.getInt("revNo4"), false);
	            if (brevs[0] != obj.getInt("revNo1")) {
	            	svn.updateFlow(devflow);
	            }
	            if (brevs[1] != obj.getInt("revNo2")) {
	            	svn.updateUi("html", devhtml);
	            }
	            if (brevs[2] != obj.getInt("revNo3")) {
	            	svn.updateUi("js", devjs);
	            }
	            if (brevs[3] != obj.getInt("revNo4")) {
	            	svn.updateUi("css", devcss);
	            }
	            System.out.println("updated");
        		// Register the merge
        		svn.merge(false, branchName);
	            System.out.println("merged");
	            // Then commit the "live" repo
        		String json = svn.commit(msg) + "";
	            System.out.println("committed");
        		return newFixedLengthResponse(json);
			} catch (SVNException | IOException | ResponseException  e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }
    /*
     *
     */

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
				int isDirty = svn.isWCDirty();
				long[] revs = svn.getLatestTrunkRevisions();
				long hrev = getHighestRevision(revs);
				long rev = svn.getLatestTrunkRevInBranch(true, branchName);
				if (hrev <= rev) {
					uptodate = true;
					return newFixedLengthResponse("{\"branch\":\"" + branchName + "\",\"hasChanges\":" + isDirty + ",\"msg\":\"Branch is up-to-date\"}");
				}
				uptodate = false;
				return newFixedLengthResponse("{\"branch\":\"" + branchName  + "\",\"hasChanges\":" + isDirty + ", \"msg\":\"\"}");
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
		            json = getDiffBranchFlows(branchFlow, liveFlow);
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
	    		JsonArray jsonArray = Json.createReader(new StringReader(requestBody)).readArray();
	    		for (JsonValue jo : jsonArray) {
    				String nm = jo.toString().replace("\"", "");
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
	           	if (uptodate) {
	        		return newFixedLengthResponse("");
	        	}
				final HashMap<String, String> map = new HashMap<String, String>();
				session.parseBody(map);
				String requestBody = map.get("postData");
				JsonObject jsonObject = Json.createReader(new StringReader(requestBody)).readObject();
				// Nodes
				JsonArray jsonArray = jsonObject.getJsonArray("nodes");
				for (Object jo : jsonArray) {
					JsonObject obj = (JsonObject) jo;
					String type = obj.getJsonObject("nodeContent").getString("type");
					mergedNodes.add(new Node(obj.getString("nodeId"), type, obj.getJsonObject("nodeContent")));
				}
				// Do final merge
				JsonArrayBuilder newFlows =Json.createArrayBuilder();
				List<Node> newNodes = new ArrayList<>();
				newNodes.addAll(mergedNodes);
				for (Node n : newNodes) {
					newFlows.add(n.getJson());
				}
				String json = parser().toJson(newFlows.build());
				svn.updateFlow(json);
				System.out.println("Flow updated");
				// HTML
				JsonValue ui = jsonObject.get("html");
				if ((ui != null) && (jsonObject.getString("html").length() > 0)) {
					svn.updateUi("html", jsonObject.getString("html"));
					System.out.println("HTML updated");
				}
				// JS
				ui = jsonObject.get("js");
				if ((ui != null) && (jsonObject.getString("js").length() > 0)) {
					svn.updateUi("js", jsonObject.getString("js"));
					System.out.println("JS updated");
				}
				// CSS
				ui = jsonObject.get("css");
				if ((ui != null) && (jsonObject.getString("css").length() > 0)) {
					svn.updateUi("css", jsonObject.getString("css"));
					System.out.println("CSS updated");
				}
				// and commit the changes
				svn.commit("PreMerge");
				// Register branch as been merged with latest trunk
				svn.merge(true, null);
				// and commit the changes
				long newRevNr = svn.commit("Merged");
				System.out.println("Merged");
				return newFixedLengthResponse("Committed in revision " + newRevNr);
			} catch (Exception e) {
				return newFixedLengthResponse(e.getMessage());
			}
        }
    }

    /*
     *
     */
    private static String getDiffTrunkFlows(String branchFlow, String liveFlow) throws UnsupportedEncodingException {

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
									if (trunk) {
										curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
										prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
									} else {
										curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
										prvhtml.append(row.getOldLine() + "<br/>");
									}
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
        		if (bf.getNodeContent().getString("type").equalsIgnoreCase("function")) {
					FlowNode removed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "only in branch", bf.getNodeContent().getString("func"), "", bf.getNodeContent());
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
        String json = "{ \"added\":" + parser().toJson(nodeOnlyInLive) + ",\"changed\":" + parser().toJson(nodeChanged) + ",\"removed\":" + parser().toJson(nodeOnlyInBranch) + "}";
        return json;
    }

    private static String getDiffBranchFlows(String branchFlow, String liveFlow) throws UnsupportedEncodingException {
        try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("flows_branch.json"));
			writer.write(branchFlow);
			writer.close();
			writer = new BufferedWriter(new FileWriter("flows_live.json"));
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
						if (bf.getNodeContent().getString("type").equalsIgnoreCase("function")) {
							// Node is a function node, so determine the diff of the function content
							if (!bf.getNodeContent().getString("func").equalsIgnoreCase(lf.getNodeContent().getString("func"))) {
								// Node content is different
								DiffRowGenerator generator = DiffRowGenerator.create()
										.showInlineDiffs(true)
										.inlineDiffByWord(true)
										.oldTag(f -> f ? "<span class=\"add\">":"</span>")
										.newTag(f -> f ? "<span class=\"del\">":"</span>")
										.build();
								List<String> curfunc = Arrays.asList(bf.getNodeContent().getString("func").split("\n"));
								List<String> prvfunc = Arrays.asList(lf.getNodeContent().getString("func").split("\n"));
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
								prvhtml.append("</div>");
								FlowNode changed = new FlowNode(bf.getTabId(), bf.getTabName(), bf.getNodeId(), bf.getNodeName(), "function code", curhtml.toString(), prvhtml.toString(), bf.getNodeContent());
								nodeChanged.add(changed);
							}
						} else // Node is not a function node
						if (!nodeExists(bf.getNodeId())) {
							mergedNodes.add(new Node(bf.getNodeId(),bf.getNodeContent().getString("type"),bf.getNodeContent()));
//							System.out.println("other nodes:  " + bf.getNodeId() + " " + bf.getNodeName() + " " + bf.getNodeContent().getString("type"));
						}
					} else if (!nodeExists(bf.getNodeId())) {
						mergedNodes.add(new Node(bf.getNodeId(), bf.getNodeContent().getString("type"), bf.getNodeContent()));
//						System.out.println("equal nodes:  " + bf.getNodeId() + " " + bf.getNodeName() + " " + bf.getNodeContent().getString("type"));
					}
				}
        	}
        	if (!found) {
        		// Node is only in live
        		if (lf.getNodeContent().getString("type").equalsIgnoreCase("function")) {
					FlowNode added = new FlowNode(lf.getTabId(), lf.getTabName(), lf.getNodeId(), lf.getNodeName(), "only in live", lf.getNodeContent().getString("func"), "", lf.getNodeContent());
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
				mergedNodes.add(new Node(bf.getNodeId(), bf.getNodeContent().getString("type"), bf.getNodeContent()));
//				System.out.println("only in branch:  " + bf.getNodeId() + " " + bf.getNodeName() + " " + bf.getNodeContent().get("type").getAsString());
			}
        }
        Collections.sort(nodeOnlyInLive);
        Collections.sort(nodeChanged);
        String json = "{ \"added\":" + parser().toJson(nodeOnlyInLive) + ",\"changed\":" + parser().toJson(nodeChanged) + "}";
        return json;
    }

	private static void getNonFlowNodes(String flow) {
		JsonArray jsonArray = Json.createReader(new StringReader(flow)).readArray();
		for (Object jo : jsonArray) {
			JsonObject obj = (JsonObject) jo;
			String type = obj.getString("type");
			JsonValue zel = obj.get("z");
			if ((zel == null) && !nodeExists(obj.getString("id"))) {
				mergedNodes.add(new Node(obj.getString("id"), type,  obj));
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

	private static List<FlowNode> getFlowNodes(String flow) {
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

	private static String determineWhatChanged(JsonObject live, JsonObject branch) {
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

    private static String getDiffHtml(String curHtml, String prvHtml) throws SVNException, UnsupportedEncodingException {
    	if (curHtml.equals(prvHtml)) {
    		return "[\"\",\"\"]";
    	}
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
			if (trunk) {
				curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
				prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			} else {
				curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
				prvhtml.append(row.getOldLine() + "<br/>");
			}
		}
		String json = "[" + parser().toJson(curhtml.toString()) + "," + parser().toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffJs(String curJs, String prvJs) throws SVNException, UnsupportedEncodingException {
    	if (curJs.equals(prvJs)) {
    		return "[\"\",\"\"]";
    	}
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
			if (trunk) {
				curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
				prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			} else {
				curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
				prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			}
		}
		String json = "[" + parser().toJson(curhtml.toString()) + "," + parser().toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffCss(String curCss, String prvCss) throws SVNException, UnsupportedEncodingException {
    	if (curCss.equals(prvCss)) {
    		return "[\"\",\"\"]";
    	}
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
			if (trunk) {
				curhtml.append(row.getNewLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
				prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			} else {
				curhtml.append(row.getNewLine().replace("<span class=\"del\">", "").replace("<span class=\"add\">", "").replace("</span>", "").replace("&lt;","<").replace("&gt;",">") + "\n");
				prvhtml.append(row.getOldLine().replace(" ", "&nbsp;").replace("span&nbsp;", "span ")).append("<br/>");
			}
		}
		String json = "[" + parser().toJson(curhtml.toString()) + "," + parser().toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffTables(String current, String previous) throws SVNException, UnsupportedEncodingException {
    	if (current.equals(previous)) {
    		return "[\"\",\"\"]";
    	}
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
		String json = "[" + parser().toJson(curhtml.toString()) + "," + parser().toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffViews(String current, String previous) throws SVNException, UnsupportedEncodingException {
    	if (current.equals(previous)) {
    		return "[\"\",\"\"]";
    	}
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
		String json = "[" + parser().toJson(curhtml.toString()) + "," + parser().toJson(prvhtml.toString()) + "]";
		return json;
    }

    private static String getDiffProcs(String current, String previous) throws SVNException, UnsupportedEncodingException {
    	if (current.equals(previous)) {
    		return "[\"\",\"\"]";
    	}
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
		String json = "[" + parser().toJson(curhtml.toString()) + "," + parser().toJson(prvhtml.toString()) + "]";
		return json;
    }

	private static long getHighestRevision(long[] revs) {
	    return Arrays.stream(revs).max().getAsLong();
	}

	public static Jsonb parser() {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		return jsonb;
	}
}

