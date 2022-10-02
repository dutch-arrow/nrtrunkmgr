package nl.das.nrdevmgr;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;

public class MyTest {

	@Test
	public void test() throws IOException, SVNException {
		Properties props = new Properties();
		props.load(new FileInputStream("src/test/resources/anydb.properties"));
		DAVRepositoryFactory.setup();
		SVNURL svnurl = SVNURL.parseURIEncoded(props.getProperty("repohost") + "/AnySens");
		SVNRepository svnrepo = DAVRepositoryFactory.create(svnurl);
		String user = "tp";
		String pwd = "Thomas1953!";
		ISVNAuthenticationManager authManager = BasicAuthenticationManager.newInstance(user, pwd.toCharArray());
		svnrepo.setAuthenticationManager(authManager);

		long rev[] = { -1 };
		svnrepo.log(new String[] { "" }, 40000L, -1, true, false, new ISVNLogEntryHandler() {
			@Override
			public void handleLogEntry(SVNLogEntry logEntry) throws
			    SVNException {
					Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
					for (String key : changedPaths.keySet()) {
						String cp = changedPaths.get(key).getCopyPath();
						if (key.startsWith(props.getProperty("branch.repourl") + "/NVDH_AS-937_20220202") && (cp != null) &&
			    			   cp.startsWith(props.getProperty("live.repourl"))) {
							System.out.println("copy-rev=" + changedPaths.get(key).getCopyRevision());
							System.out.println(changedPaths.get(key));
							long r = changedPaths.get(key).getCopyRevision();
							rev[0] = r > rev[0] ? r : rev[0];
						}
					}
			    }
			}
		);
		System.out.println("last trunk merge rev=" + rev[0]);

		rev[0] = -1;
		String path = props.getProperty("live.repourl");
		svnrepo.log(new String[] { "" }, 50000L, -1, true, false, new ISVNLogEntryHandler() {
			@Override
			public void handleLogEntry(SVNLogEntry logEntry) throws
			    SVNException {
			       Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
			       for (String key : changedPaths.keySet()) {
			    	   if (key.startsWith(path)) {
			    		   long r = logEntry.getRevision();
			    		   rev[0] = r > rev[0] ? r : rev[0];
			    	   }
			       }
			    }
			}
		);
		System.out.println("latest trunk rev=" + rev[0]);
	}
}
