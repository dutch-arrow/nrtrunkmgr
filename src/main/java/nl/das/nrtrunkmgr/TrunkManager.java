/**
 *******************************************************************************************
 **
 **  @filename       CompareFlows.java
 **  @brief
 **
 **  @copyright      (c) Core|Vision B.V.,
 **                  Cereslaan 10b,
 **                  5384 VT  Heesch,
 **                  The Netherlands,
 **                  All Rights Reserved
 **
 **  @author         tom
 **  @svnversion     $Date: 2021-12-18 15:34:55 +0100 (Sat, 18 Dec 2021) $
 **                  $Revision: 50048 $
 **
 *******************************************************************************************
 */

package nl.das.nrtrunkmgr;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParseResult;

/**
 * See https://svnkit.com/javadoc/index.html?overview-summary.html See
 * See https://wiki.svnkit.com/Managing_A_Working_Copy
 *
 * @author tom
 *
 */
@Command(name = "TrunkManager", version = "(c) 2022, Dutch Arrow Software")
public class TrunkManager {

	@Parameters(paramLabel = "<properties filepath>", defaultValue = "trunkmgr.properties", description = "Path of the properties file")
	static String propFilePath;

	public static void main(String[] args) throws IOException {
		TrunkManager app = new TrunkManager();
	     try {
	         ParseResult parseResult = new CommandLine(app).parseArgs(args);
	         if (!CommandLine.printHelpIfRequested(parseResult)) {
	 			if (!Files.exists(Paths.get(propFilePath))) {
	 				System.err.println("Properties file '" + propFilePath + "' not found");
				} else {
					app.runProgram();
				}
	         }
	     } catch (ParameterException ex) { // command line arguments could not be parsed
	         System.err.println(ex.getMessage());
	         ex.getCommandLine().usage(System.err);
	     }
	}

	public void runProgram() {
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(propFilePath));
			Webserver srv = Webserver.getInstance(props);
			srv.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
