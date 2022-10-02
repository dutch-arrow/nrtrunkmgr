/**
 *******************************************************************************************
 **
 **  @filename       SvnActions.java
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

package nl.das.nrdevmgr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNConflictChoice;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnCommit;
import org.tmatesoft.svn.core.wc2.SvnCopySource;
import org.tmatesoft.svn.core.wc2.SvnExport;
import org.tmatesoft.svn.core.wc2.SvnGetInfo;
import org.tmatesoft.svn.core.wc2.SvnGetMergeInfo;
import org.tmatesoft.svn.core.wc2.SvnGetStatus;
import org.tmatesoft.svn.core.wc2.SvnInfo;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnLog;
import org.tmatesoft.svn.core.wc2.SvnMerge;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRemoteCopy;
import org.tmatesoft.svn.core.wc2.SvnRemoteDelete;
import org.tmatesoft.svn.core.wc2.SvnResolve;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnStatus;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.tmatesoft.svn.core.wc2.SvnUpdate;

/**
 * This class manipulates the Subversion (SVN) functions needed for the AnyDB (NodeRED) managers
 */
/**
 *
 */
public class SvnActions {

	/**
	 * Property file with the following properties:
	 *
	 * host=          Name of the host where the manager runs on (e.g. localhost)
	 * port=          Port number where the manager runs on (e.g.4545)
	 * repohost=      SVN host URL (e.g. http://localhost)
	 * path.trunk=    Path of the SVN trunk (e.g. /svn/test/trunk/nodered-live)
	 * path.branches= Path of the SVN branches (e.g. /svn/test/branches)
	 * workdir=       Absolute path of the NodeRED work directory (e.g. /homes/tom/.node-red)
	 */
	private Properties props;
	private SvnOperationFactory svnOperationFactory;
	private SVNURL trunkUrl;
	private SVNURL trunkSql;
	private SVNURL branchUrl;
	private String workdir;
	private String user;
	private String pwd;
	private String uiPath;

	/**
	 * Constructor
	 *
	 * @param properties
	 * @throws SVNException
	 */
	public SvnActions(Properties properties) throws SVNException {
		this.props = properties;
		this.trunkUrl = SVNURL.parseURIEncoded(this.props.getProperty("repohost") + this.props.getProperty("path.trunk"));
		this.branchUrl = SVNURL.parseURIEncoded(this.props.getProperty("repohost") + this.props.getProperty("path.branches"));
		this.trunkSql = SVNURL.parseURIEncoded(this.props.getProperty("repohost") + this.props.getProperty("path.sql"));
		this.workdir = this.props.getProperty("workdir");
//		DAVRepositoryFactory.setup();
		this.user = this.props.getProperty("username");
		this.pwd = this.props.getProperty("password");
		ISVNAuthenticationManager authManager = BasicAuthenticationManager.newInstance(this.user, this.pwd.toCharArray());
		this.svnOperationFactory = new SvnOperationFactory();
		this.svnOperationFactory.setAuthenticationManager(authManager);

	}

	/**
	 * Get the name of the branch that is checked out in the workdir
	 *
	 * @return name of the branch
	 * @throws IOException
	 */
	public String getBranch() {
		String branchName = "";
		List<SvnInfo> infos = new ArrayList<>();
		SvnGetInfo gi = this.svnOperationFactory.createGetInfo();
		gi.addTarget(SvnTarget.fromFile(new File(this.props.getProperty("workdir"))));
		try {
			gi.run(infos);
			branchName = infos.get(0).getUrl().getPath().replace(this.props.getProperty("path.branches") + "/", "");
			String flow = getBranchFlow(branchName, 0, true);
			if (flow != "") {
				this.uiPath = getUiUrl(flow);
			} else {
				this.uiPath = "";
			}
		} catch (SVNException | IOException e) {
		}
		return branchName;
	}

	/**
	 * Get a list of all branches with author equal to the current user
	 *
	 * @return list of branch names
	 * @throws SVNException
	 * @throws IOException
	 */
	public List<String> getAllBranches() throws SVNException, IOException {
		String flow = getTrunkFlow(-1);
		if (flow != "") {
			this.uiPath = getUiUrl(flow);
		} else {
			this.uiPath = "";
		}
		List<String> brs = new ArrayList<>();
		SvnList lst = this.svnOperationFactory.createList();
		lst.addTarget(SvnTarget.fromURL(this.branchUrl));
		lst.setDepth(SVNDepth.IMMEDIATES);
		lst.setRevision(SVNRevision.HEAD);
		List<SVNDirEntry> dirs = new ArrayList<>();
		lst.run(dirs);
		for (SVNDirEntry de : dirs) {
			String br = de.getRelativePath();
			if (br != "") {
				brs.add(br);
			}
		}
		return brs;
	}

	/**
	 * Get a list of all branches with author equal to the current user
	 *
	 * @return list of branch names
	 * @throws SVNException
	 */
	public List<String> getMyBranches() throws SVNException {
		List<String> brs = new ArrayList<>();
		SvnList lst = this.svnOperationFactory.createList();
		SVNURL brsurl = SVNURL.parseURIEncoded(this.props.getProperty("repohost") + this.props.getProperty("path.branches"));
		lst.addTarget(SvnTarget.fromURL(brsurl));
		lst.setDepth(SVNDepth.IMMEDIATES);
		lst.setRevision(SVNRevision.HEAD);
		List<SVNDirEntry> dirs = new ArrayList<>();
		lst.run(dirs);
		for (SVNDirEntry de : dirs) {
			String br = de.getRelativePath();
			if((br != "") && de.getAuthor().equalsIgnoreCase(this.props.getProperty("username"))) {
				brs.add(br);
			}
		}
		return brs;
	}
	/**
	 * Create a new branch in the SVN repository from the trunk
	 *
	 * @param name Name of the branch
	 * @throws SVNException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void createBranch(String name) throws SVNException, IOException, InterruptedException {
		// Create branch
		SvnRemoteCopy remoteCopy = this.svnOperationFactory.createRemoteCopy();
		SVNURL brurl = SVNURL.parseURIEncoded(this.props.getProperty("repohost") + this.props.getProperty("path.branches") + "/" + name);
		SVNURL trurl = this.trunkUrl;
		SvnCopySource src = SvnCopySource.create(SvnTarget.fromURL(trurl), SVNRevision.HEAD);
		remoteCopy.addCopySource(src);
		remoteCopy.addTarget(SvnTarget.fromURL(brurl));
		remoteCopy.setCommitMessage("Branch created");
		remoteCopy.setMakeParents(true);
		remoteCopy.run();

		// Checkout branch in workdir
		// Clear work folder
		SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) {
					throw exc;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		};
		Files.walkFileTree(Paths.get(this.props.getProperty("workdir")), visitor);

		// Checkout branch in workdir
		SvnCheckout checkout = this.svnOperationFactory.createCheckout();
		checkout.addTarget(SvnTarget.fromFile(new File(this.props.getProperty("workdir"))));
		checkout.setSource(SvnTarget.fromURL(brurl));
		checkout.run();
		// Run "npm install" in workdir
		boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
		ProcessBuilder builder = new ProcessBuilder();
		if (isWindows) {
		    builder.command("cmd.exe", "/c", "npm install");
		} else {
		    builder.command("sh", "-c", "npm install");
		}
		builder.directory(new File(this.props.getProperty("workdir")));
		Process process = builder.start();
		StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), l -> {});
		Executors.newSingleThreadExecutor().submit(streamGobbler);
		int exitCode = process.waitFor();
		assert exitCode == 0;
	}

	/**
	 * Removes a branch from the SVN repository
	 *
	 * @param name name of branch to remove
	 * @throws SVNException
	 */
	public void removeBranch(String name) throws SVNException {
		SvnRemoteDelete remoteDel = this.svnOperationFactory.createRemoteDelete();
		SVNURL brurl = SVNURL.parseURIEncoded(this.props.getProperty("repohost") + this.props.getProperty("path.branches") + "/" + name);
		remoteDel.addTarget(SvnTarget.fromURL(brurl));
		remoteDel.setCommitMessage("No longer needed");
		remoteDel.run();
	}

	/**
	 * Determines if in the trunk version work folder there are uncommitted changes in the NodeRED files:
	 * <ul>
	 * <li>flows.json</li>
	 * <li>uibuilder/[uiPath]/src/index.html</li>
	 * <li>uibuilder/[uiPath]/src/index.js</li>
	 * <li>uibuilder/[uiPath]/src/index.css</li>
	 * </ul>
	 *
	 * @return true if dirty
	 * @throws SVNException
	 */
	public int isWCDirty() throws SVNException {
		int changes = 0;
		List<SvnStatus> sts = new ArrayList<>();
		SvnGetStatus status = this.svnOperationFactory.createGetStatus();
		status.addTarget(SvnTarget.fromFile(new File(this.workdir + "/flows.json")));
		status.run(sts);
		if (sts.get(0).getNodeStatus() == SVNStatusType.STATUS_MODIFIED) {
			changes = 1;
		}
		if (Files.exists(Paths.get(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.html"))) {
			if ((changes == 0) || (changes == 1)) {
				sts = new ArrayList<>();
				status = this.svnOperationFactory.createGetStatus();
				status.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.html")));
				status.run(sts);
				if (sts.get(0).getNodeStatus() == SVNStatusType.STATUS_MODIFIED) {
					changes += 2;
				}
			}
			if ((changes == 0) || (changes == 1)) {
				sts = new ArrayList<>();
				status = this.svnOperationFactory.createGetStatus();
				status.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.js")));
				status.run(sts);
				if (sts.get(0).getNodeStatus() == SVNStatusType.STATUS_MODIFIED) {
					changes += 2;
				}
			}
			if ((changes == 0) || (changes == 1)) {
				sts = new ArrayList<>();
				status = this.svnOperationFactory.createGetStatus();
				status.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.css")));
				status.run(sts);
				if (sts.get(0).getNodeStatus() == SVNStatusType.STATUS_MODIFIED) {
					changes += 2;
				}
			}
		}
		return changes;
	}

	/**
	 * Get the latest revision number of the trunk
	 *
	 * @return revision number
	 * @throws SVNException
	 */
	public long getLatestTrunkRevision() throws SVNException {
		List<SvnInfo> infos = new ArrayList<>();
		SvnGetInfo gi = this.svnOperationFactory.createGetInfo();
		gi.addTarget(SvnTarget.fromURL(this.trunkUrl));
		gi.run(infos);
		return infos.get(infos.size() - 1).getLastChangedRevision();
	}

	/**
	 * From the trunk-repo get the latest revision number of the NodeRED files:
	 * <ul>
	 * <li>flows.json</li>
	 * <li>uibuilder/[uiPath]/src/index.html</li>
	 * <li>uibuilder/[uiPath]/src/index.js</li>
	 * <li>uibuilder/[uiPath]/src/index.css</li>
	 * </ul>
	 *
	 * @return Array of 4 longs
	 * @throws SVNException
	 * @throws UnsupportedEncodingException
	 */
	public long[] getLatestTrunkRevisions() throws SVNException, UnsupportedEncodingException {
		// flows.json, index.html, index.js, index.css
		long[] rev = {0,0,0,0};
		List<SvnInfo> infos = new ArrayList<>();
		SvnGetInfo gi = this.svnOperationFactory.createGetInfo();
		gi.addTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("flows.json", false)));
		gi.run(infos);
		rev[0] = infos.get(infos.size() - 1).getLastChangedRevision();
		try {
			infos = new ArrayList<>();
			gi = this.svnOperationFactory.createGetInfo();
			gi.addTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("/uibuilder/" + this.uiPath + "/src/index.html", false)));
			infos = new ArrayList<>();
			gi.run(infos);
			rev[1] = infos.get(infos.size() - 1).getLastChangedRevision();
		} catch (SVNException e) {
//			System.out.println("getLatestTrunkRevisions: " + e.getMessage());
			if (!(e.getMessage().contains("uibuilder/src/index") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		try {
			gi = this.svnOperationFactory.createGetInfo();
			gi.addTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("/uibuilder/" + this.uiPath + "/src/index.js", false)));
			infos = new ArrayList<>();
			gi.run(infos);
			rev[2] = infos.get(infos.size() - 1).getLastChangedRevision();
		} catch (SVNException e) {
			if (!(e.getMessage().contains("uibuilder/src/index") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		try {
			gi = this.svnOperationFactory.createGetInfo();
			gi.addTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("/uibuilder/" + this.uiPath + "/src/index.css", false)));
			infos = new ArrayList<>();
			gi.run(infos);
			rev[3] = infos.get(infos.size() - 1).getLastChangedRevision();
		} catch (SVNException e) {
			if (!(e.getMessage().contains("uibuilder/src/index") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		return rev;
	}

	/**
	 * From the branch-repo get the latest revision number of the NodeRED files:
	 * <ul>
	 * <li>flows.json</li>
	 * <li>/uibuilder/[uiPath]/src/index.html</li>
	 * <li>/uibuilder/[uiPath]/src/index.js</li>
	 * <li>/uibuilder/[uiPath]/src/index.css</li>
	 * </ul>
	 *
	 * @return Array of 4 longs
	 * @throws SVNException
	 * @throws UnsupportedEncodingException
	 */
	public long[] getLatestBranchRevisions(String branchName) throws SVNException, UnsupportedEncodingException {
		// flows.json, index.html, index.js, index.css
		long[] rev = {0,0,0,0};
		SvnGetInfo gi = this.svnOperationFactory.createGetInfo();
		gi.addTarget(SvnTarget.fromURL(this.branchUrl.appendPath(branchName + "/flows.json", false) ));
		SvnInfo info = gi.run();
		rev[0] = info.getLastChangedRevision();
		try {
			gi = this.svnOperationFactory.createGetInfo();
			gi.addTarget(SvnTarget.fromURL(this.branchUrl.appendPath(branchName + "/uibuilder/" + this.uiPath + "/src/index.html", false)));
			info = gi.run();
			rev[1] = info.getLastChangedRevision();
		} catch (SVNException e) {
//			System.out.println("getLatestBranchRevisions: " + e.getMessage());
			if (!(e.getMessage().contains("uibuilder") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		try {
			gi = this.svnOperationFactory.createGetInfo();
			gi.addTarget(SvnTarget.fromURL(this.branchUrl.appendPath(branchName + "/uibuilder/" + this.uiPath + "/src/index.js", false)));
			info = gi.run();
			rev[2] = info.getLastChangedRevision();
			} catch (SVNException e) {
				if (!(e.getMessage().contains("uibuilder/src/index") &&
						(e.getMessage().contains("non-existent") ||
						 e.getMessage().contains("was not found.") ||
						 e.getMessage().contains("path not found:")))) {
					throw e;
				}
			}
		try {
			gi = this.svnOperationFactory.createGetInfo();
			gi.addTarget(SvnTarget.fromURL(this.branchUrl.appendPath(branchName + "/uibuilder/" + this.uiPath + "/src/index.css", false)));
			info = gi.run();
			rev[3] = info.getLastChangedRevision();
			} catch (SVNException e) {
				if (!(e.getMessage().contains("uibuilder/src/index") &&
						(e.getMessage().contains("non-existent") ||
						 e.getMessage().contains("was not found.") ||
						 e.getMessage().contains("path not found:")))) {
					throw e;
				}
		}
		return rev;
	}

	/**
	 * Get the latest revision number of trunk that is merged into the branch
	 *
	 * @param fromWC from Working Copy (true) or Repository (false)?
	 * @return the revision number as long.
	 * @throws SVNException
	 */
	public long getLatestTrunkRevInBranch(boolean fromWC, String branchName) throws SVNException {
		long rev = 0;
		SvnGetMergeInfo gmi = this.svnOperationFactory.createGetMergeInfo();
		if(fromWC) {
			gmi.setSingleTarget(SvnTarget.fromFile(new File(this.workdir)));
		} else {
			gmi.setSingleTarget(SvnTarget.fromURL(this.branchUrl.appendPath(branchName, true)));
		}
		Map<SVNURL,SVNMergeRangeList> map = gmi.run();
		if (map != null) {
			for (SVNURL url : map.keySet()) {
				if (url.toString().equalsIgnoreCase(this.trunkUrl.toString())) {
					SVNMergeRangeList lst = map.get(url);
					for (SVNMergeRange r : lst.getRangesAsList()) {
						rev = r.getEndRevision() > rev ? r.getEndRevision() : rev;
					}
				}
			}
		} else {
			rev = getFirstLiveRevInBranch(fromWC, branchName);
		}
		return rev;
	}

	/**
	 * @param env "trunk" or branchName
	 * @param type 'f' (flows.json), 'h' (index.html), 'j' (index.js), 'c' (index.css)
	 * @return
	 * @throws SVNException
	 */
	public List<Long> getAllRevisionNumbers(String env, char type) throws SVNException {
		List<Long> revs = new ArrayList<>();
		SvnLog log = this.svnOperationFactory.createLog();
		try {
			if (env.equalsIgnoreCase("trunk")) {
				switch (type) {
				case 'f': {
					log.setSingleTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("flows.json", false)));
					break;
				}
				case 'h': {
					log.setSingleTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("uibuilder/" + this.uiPath + "/src/index.html", false)));
					break;
				}
				case 'j': {
					log.setSingleTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("uibuilder/" + this.uiPath + "/src/index.js", false)));
					break;
				}
				case 'c': {
					log.setSingleTarget(SvnTarget.fromURL(this.trunkUrl.appendPath("uibuilder/" + this.uiPath + "/src/index.css", false)));
					break;
				}
				}
			} else {
				switch (type) {
				case 'f': {
					log.setSingleTarget(SvnTarget.fromURL(this.branchUrl.appendPath(env + "/flows.json", false)));
					break;
				}
				case 'h': {
					log.setSingleTarget(SvnTarget.fromURL(this.branchUrl.appendPath(env + "/uibuilder/" + this.uiPath + "/src/index.html", false)));
					break;
				}
				case 'j': {
					log.setSingleTarget(SvnTarget.fromURL(this.branchUrl.appendPath(env + "/uibuilder/" + this.uiPath + "/src/index.js", false)));
					break;
				}
				case 'c': {
					log.setSingleTarget(SvnTarget.fromURL(this.branchUrl.appendPath(env + "/uibuilder/" + this.uiPath + "/src/index.css", false)));
					break;
				}
				}
				log.setSingleTarget(SvnTarget.fromURL(this.branchUrl.appendPath(env + "/flows.json", false)));
			}
			log.setRevisionRanges(Collections.singleton(SvnRevisionRange.create(SVNRevision.create(1), SVNRevision.HEAD)));
			Collection<SVNLogEntry> les = log.run(null);
			for (SVNLogEntry le : les) {
				revs.add(le.getRevision());
			}
		} catch (SVNException e) {
//			System.out.println("getAllRevisionNumbers: " + e.getMessage());
			if (!(e.getMessage().contains("uibuilder") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		return revs;
	}

	/**
	 * @param type 'p' (procedures.sql'), 'v' (views.sql) or 't' (table.sql)
	 * @return
	 * @throws SVNException
	 */
	public List<Long> getSqlRevisions(char type) throws SVNException {
		List<Long> revs = new ArrayList<>();
		try {
			SvnLog log = this.svnOperationFactory.createLog();
			log.setRevisionRanges(Collections.singleton(SvnRevisionRange.create(SVNRevision.create(1), SVNRevision.HEAD)));
			switch (type) {
			case 'p': {
				log.setSingleTarget(SvnTarget.fromURL(this.trunkSql.appendPath("scripts/procedures.sql", false)));
				break;
			}
			case 'v': {
				log.setSingleTarget(SvnTarget.fromURL(this.trunkSql.appendPath("scripts/views.sql", false)));
				break;
			}
			case 't': {
				log.setSingleTarget(SvnTarget.fromURL(this.trunkSql.appendPath("scripts/tables.sql", false)));
				break;
			}
			}
			Collection<SVNLogEntry> les = log.run(null);
			for (SVNLogEntry le : les) {
				revs.add(le.getRevision());
			}
		} catch (SVNException e) {
	//		System.out.println("getAllRevisionNumbers: " + e.getMessage());
			if (!(e.getMessage().contains("scripts/") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		return revs;
	}

	/**
	 * Get the content of the flows.json file in the trunk
	 *
	 * @param revision Revision number of the flows.json file
	 * @return the content of the flows.json file
	 * @throws SVNException
	 * @throws IOException
	 */
	public String getTrunkFlow(long revision) throws SVNException, IOException {
		String content = "";
		if (revision == 0) {
			// Get Workdir file
			content = new String(Files.readAllBytes(Paths.get(this.workdir + "/flows.json")));
		} else {
			File tmpdir = Files.createTempDirectory("anydb-").toFile();
			SvnExport export = this.svnOperationFactory.createExport();
			export.setSingleTarget(SvnTarget.fromFile(tmpdir));
			export.setForce(true);
			if (revision == -1) {
				// Get HEAD revision of flows.json in repo
				export.setSource(SvnTarget.fromURL(this.trunkUrl.appendPath("flows.json", false), SVNRevision.HEAD));
				export.run();
			} else {
				// Get given revision of flows.json in repo
				try {
					export.setSource(SvnTarget.fromURL(this.trunkUrl.appendPath("flows.json", false), SVNRevision.create(revision)));
					export.run();
				} catch (SVNException e) {
					if (!e.getMessage().startsWith("svn: E170000:")) {
						throw e;
					}
					export.setSource(SvnTarget.fromURL(this.trunkUrl.appendPath("flows.json", false), SVNRevision.create(-1)));
					export.run();
				}
			}
			content = new String(Files.readAllBytes(Paths.get(tmpdir.getAbsolutePath() + "/flows.json")));
		}
		return content;
	}

	/**
	 * Get the content of the UI file determined by the type
	 *
	 * @param revision Revision number of the index.[type] file
	 * @param type 'html', 'js' or 'css'
	 * @return the content of the index-file
	 * @throws SVNException
	 * @throws IOException
	 */
	public String getTrunkUi(String type, long revision) throws SVNException, IOException {
		String content = "";
		try {
			if (revision == 0) {
				// Get Workdir file
				content = new String(Files.readAllBytes(Paths.get(this.workdir + "/uibuilder/" + this.uiPath + "/src/index." + type)));
			} else {
				File tmpdir = Files.createTempDirectory("anydb-").toFile();
				SvnExport export = this.svnOperationFactory.createExport();
				export.setSingleTarget(SvnTarget.fromFile(tmpdir));
				export.setForce(true);
				if (revision == -1) {
					export.setSource(SvnTarget.fromURL(this.trunkUrl.appendPath("uibuilder/" + this.uiPath + "/src/index." + type, false), SVNRevision.HEAD));
				} else {
					export.setSource(SvnTarget.fromURL(this.trunkUrl.appendPath("uibuilder/" + this.uiPath + "/src/index." + type, false), SVNRevision.create(revision)));
				}
				export.run();
				content = new String(Files.readAllBytes(Paths.get(tmpdir.getAbsolutePath() + "/index." + type)));
			}
		} catch (SVNException e) {
			if (!(e.getMessage().contains("uibuilder") &&
					(e.getMessage().contains("non-existent") ||
					 e.getMessage().contains("doesn't exist") ||
					 e.getMessage().contains("was not found.") ||
					 e.getMessage().contains("path not found:")))) {
				throw e;
			}
		}
		return content;
	}

	/**
	 * Get the content of the SQL file determined by the type
	 *
	 * @param revision Revision number of the SQL [type] file
	 * @param type 'procedures', 'tables' or 'views'
	 * @return the content of the SQL-file
	 * @throws SVNException
	 * @throws IOException
	 */
	public String getTrunkSql(long revision, String type) throws SVNException, IOException {
		File tmpdir = Files.createTempDirectory("anydb-").toFile();
		SvnExport export = this.svnOperationFactory.createExport();
		export.setSingleTarget(SvnTarget.fromFile(tmpdir));
		export.setForce(true);
		try {
			export.setSource(SvnTarget.fromURL(this.trunkSql.appendPath("scripts/" + type + ".sql", false), SVNRevision.create(revision)));
			export.run();
		} catch (SVNException e) {
			if (!e.getMessage().startsWith("svn: E170000:")) {
				throw e;
			}
			export.setSource(SvnTarget.fromURL(this.branchUrl.appendPath("scripts/" + type + ".sql", false), SVNRevision.create(-1)));
			export.run();
		}
		String content = new String(Files.readAllBytes(Paths.get(tmpdir.getAbsolutePath() + "/" + type + ".sql")));
		return content;
	}

	/**
	 * Get the flow.json content of the branch in the work folder.
	 *
	 * @param branch name of the branch
	 * @param revno revision number
	 * @param fromWC from Working Copy (true) or Repository (false)?
	 * @return the content of the flows.json file
	 * @throws IOException
	 * @throws SVNException
	 */
	public String getBranchFlow(String branch, long revno, boolean fromWC) throws IOException, SVNException {
		String content = "";
		if (fromWC) {
			content = new String(Files.readAllBytes(Paths.get(this.workdir + "/flows.json")));
		} else {
			File tmpdir = Files.createTempDirectory("nodered-").toFile();
			SvnExport export = this.svnOperationFactory.createExport();
			export.setSingleTarget(SvnTarget.fromFile(tmpdir));
			export.setForce(true);
			try {
				export.setSource(SvnTarget.fromURL(this.branchUrl.appendPath(branch + "/flows.json", false), SVNRevision.create(revno)));
				export.run();
			} catch (SVNException e) {
				if (!e.getMessage().startsWith("svn: E170000:")) {
					throw e;
				}
				export.setSource(SvnTarget.fromURL(this.branchUrl.appendPath(branch + "/flows.json", false), SVNRevision.create(-1)));
				export.run();
			}
			content = new String(Files.readAllBytes(Paths.get(tmpdir.getAbsolutePath() + "/flows.json")));
		}

		return content;
	}

	/**
	 * Get the content of the index.[type] file of the branch in the work folder.
	 *
	 * @param type "html", "js" or "css"
	 * @return the content of the file
	 * @throws SVNException
	 */
	public String getBranchUi(String type, String branch, long revno, boolean fromWC) throws SVNException {
		String content = "";
		try {
			if (fromWC) {
				content = new String(Files.readAllBytes(Paths.get(this.workdir + "/uibuilder/" + this.uiPath + "/src/index." + type)));
			} else {
				try {
					File tmpdir = Files.createTempDirectory("nodered-").toFile();
					SvnExport export = this.svnOperationFactory.createExport();
					export.setSingleTarget(SvnTarget.fromFile(tmpdir));
					export.setForce(true);
					export.setSource(SvnTarget.fromURL(this.branchUrl.appendPath(branch + "/uibuilder/" + this.uiPath + "/src/index." + type, false), SVNRevision.create(revno)));
					export.run();
					content = new String(Files.readAllBytes(Paths.get(tmpdir.getAbsolutePath() + "/index." + type)));
				} catch (SVNException e) {
					if (!(e.getMessage().contains("uibuilder") &&
							(e.getMessage().contains("non-existent") ||
							 e.getMessage().contains("doesn't exist") ||
							 e.getMessage().contains("was not found.") ||
							 e.getMessage().contains("path not found:")))) {
						throw e;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	/**
	 * Write new content to the flows.json file in the work folder
	 *
	 * @param flow the new content
	 * @throws IOException
	 */
	public void updateFlow(String flow) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.workdir + "/flows.json"));
        writer.write(flow);
        writer.close();
	}

	/**
	 * Write new content to the index.[type]  file in the work folder.
	 *
	 * @param type "html", "js" or "css"
	 * @param content the new content
	 * @throws IOException
	 */
	public void updateUi(String type, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.workdir + "/uibuilder/" + this.uiPath + "/src/index." + type));
        writer.write(content);
        writer.close();
	}

	/**
	 * Register the merge in the work folder
	 *
	 * @throws SVNException
	 */
	public void merge(boolean trunkInBranch, String branch) throws SVNException {
		SvnMerge merge = this.svnOperationFactory.createMerge();
		merge.addTarget(SvnTarget.fromFile(new File(this.workdir)));
		if (trunkInBranch) {
			merge.setSource(SvnTarget.fromURL(this.trunkUrl), true);
		} else {
			merge.setSource(SvnTarget.fromURL(this.branchUrl.appendPath(branch, true)), true);
		}
		merge.setRecordOnly(true);
		merge.setAllowMixedRevisions(true);
		merge.run();
		SvnResolve resolve = this.svnOperationFactory.createResolve();
		resolve.setConflictChoice(SVNConflictChoice.MINE_FULL);
		resolve.addTarget(SvnTarget.fromFile(new File(this.workdir + "/flows.json")));
		resolve.run();
		if (Files.exists(Paths.get(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.html"))) {
			resolve = this.svnOperationFactory.createResolve();
			resolve.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.html")));
			resolve.run();
			resolve = this.svnOperationFactory.createResolve();
			resolve.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.js")));
			resolve.run();
			resolve = this.svnOperationFactory.createResolve();
			resolve.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.css")));
			resolve.run();
		}
	}

	/**
	 * Commit the work folder
	 *
	 * @param commitMessage
	 * @return the new revision number
	 * @throws SVNException
	 */
	public long commit(final String commitMessage) throws SVNException {
		SvnCommit commit = this.svnOperationFactory.createCommit();
		commit.setCommitMessage(commitMessage);
		commit.addTarget(SvnTarget.fromFile(new File(this.workdir + "/flows.json")));
		if (Files.exists(Paths.get(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.html"))) {
			commit.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.html")));
			commit.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.js")));
			commit.addTarget(SvnTarget.fromFile(new File(this.workdir + "/uibuilder/" + this.uiPath + "/src/index.css")));
		}
		commit.addTarget(SvnTarget.fromFile(new File(this.workdir)));
		SVNCommitInfo ci = commit.run();
		SvnUpdate update = this.svnOperationFactory.createUpdate();
		update.addTarget(SvnTarget.fromFile(new File(this.workdir)));
		update.run();
		return ci.getNewRevision();
	}

	/**
	 * Show a log-entry on the console
	 *
	 * @param logEntry
	 */
	public void dumpLogEntry(SVNLogEntry logEntry) {
		System.out.println("Author    : " + logEntry.getAuthor());
		System.out.println("Message   : " + logEntry.getMessage());
		System.out.println("Date      : " + logEntry.getDate().toString());
		System.out.println("Revision  : " + logEntry.getRevision());
		System.out.println("Properties: ");
		SVNProperties p = logEntry.getRevisionProperties();
		for (String key : logEntry.getRevisionProperties().nameSet()) {
			SVNPropertyValue val = p.getSVNPropertyValue(key);
			if ((val != null) && p.getSVNPropertyValue(key).isBinary()) {
				System.out.printf("  %20s: %s\n" , key, Utils.bytesToHex(p.getSVNPropertyValue(key).getBytes()));
			} else {
				System.out.printf("  %20s: %s\n", key, p.getSVNPropertyValue(key).getString());
			}
		}
		System.out.println("     Paths: ");
		Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
		for (String key : changedPaths.keySet()) {
			SVNLogEntryPath lep = changedPaths.get(key);
			System.out.printf("            Type: %c Path: %-80s CopyPath: %-30s CopyRev# %d Kind: %s\n", lep.getType(), lep.getPath(), lep.getCopyPath(), lep.getCopyRevision(), lep.getKind().toString());
		}
		System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
	}

	/**
	 * Determine the URL path of the UI
	 *
	 * @param flow
	 * @return
	 */
	private String getUiUrl(String flow) {
		String url = "";
		JsonArray jsonArray = Json.createReader(new StringReader(flow)).readArray();
		for (JsonValue jo : jsonArray) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject obj = (JsonObject) jo;
				String type = obj.getString("type");
				if (type.equalsIgnoreCase("uibuilder")) {
					url = obj.getString("url");
				}
			}
		}
		return url;
	}

	/**
	 * Determine the first trunk revision in the current branch
	 *
	 * @return
	 * @throws SVNException
	 */
	private long getFirstLiveRevInBranch(boolean fromWC, String branchName) throws SVNException {
		SvnLog log = this.svnOperationFactory.createLog();
		if (fromWC) {
			log.addTarget(SvnTarget.fromFile(new File(this.props.getProperty("workdir")), SVNRevision.HEAD));
		} else {
			log.addTarget(SvnTarget.fromURL(this.branchUrl.appendPath(branchName, true), SVNRevision.HEAD));
		}
		log.addRange(SvnRevisionRange.create(SVNRevision.HEAD, SVNRevision.create(1)));
		log.setStopOnCopy(true);
		log.setDiscoverChangedPaths(false);
		log.setUseMergeHistory(false);
		List<SVNLogEntry> les = new ArrayList<>();
		log.run(les);
		return les.get(les.size() - 1).getRevision();
	}

	/**
	 * Gobble the inputstream through the given consumer.
	 */
	private static class StreamGobbler implements Runnable {
	    private InputStream inputStream;
	    private Consumer<String> consumer;

	    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
	        this.inputStream = inputStream;
	        this.consumer = consumer;
	    }

	    @Override
	    public void run() {
	        new BufferedReader(new InputStreamReader(this.inputStream)).lines()
	          .forEach(this.consumer);
	    }
	}

}
