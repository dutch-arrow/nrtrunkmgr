/**
 *******************************************************************************************
 **
 **  @filename       FlowsMetadata.java
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


package nl.das.nrtrunkmgr.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 *
 */
public class FlowsMetadata {

	private static Map<String, List<String>> metaToMerge = new HashMap<>();
	// Define the elements that need to be merged
	// value types:
	// - S = String
	// - I = Integer
	// - B = boolean
	// - O = JsonObject
	// - A = array of
	static {
		metaToMerge.put("tab", Arrays.asList("label"));
		metaToMerge.put("mqtt in", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("mqtt out", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("status", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("link out", Arrays.asList("name", "links", "x", "y", "z", "wires"));
		metaToMerge.put("link in", Arrays.asList("name", "links", "x", "y", "z", "wires"));
		metaToMerge.put("function", Arrays.asList("name", "func", "initialize", "finalize", "libs", "outputs", "x", "y", "z", "wires"));
		metaToMerge.put("inject", Arrays.asList("name", "props", "repeat", "crontab", "once", "onceDelay", "topic", "payloadType", "outputs", "x", "y", "z", "wires"));
		metaToMerge.put("change", Arrays.asList("name", "rules", "action", "property", "from", "to", "reg", "x", "y", "z", "wires"));
		metaToMerge.put("mysql", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("join", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("delay", Arrays.asList("name", "pauseType", "timeout", "timeoutUnits", "rate", "nbRateUnits", "rateUnits", "randomFirst", "randomLast", "randomUnits", "drop", "allowrate", "outputs", "x", "y", "z", "wires"));
		metaToMerge.put("http in", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("http response", Arrays.asList("S:name", "x", "y", "z", "wires"));
		metaToMerge.put("tcp in", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("tcp out", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("udp in", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("udp out", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("switch", Arrays.asList("name", "rules", "property", "propertyType", "checkAll", "repair", "outputs", "x", "y", "z", "wires"));
		metaToMerge.put("comment", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("uibuilder", Arrays.asList("name", "x", "y", "z", "wires"));
		metaToMerge.put("mqtt-broker", Arrays.asList(""));
		metaToMerge.put("catch", Arrays.asList(""));
	}

	public static JsonObject merge(JsonObject master, JsonObject slave) {
		JsonObjectBuilder merged = Json.createObjectBuilder();
		String type = master.get("type").toString();
		System.out.println("Node type=" + type);
		List<String> meta = metaToMerge.get(type);
		if (meta == null) {
			System.out.println("Unregistered Node Type: " + type);
		}
		for (String key : master.keySet()) {
			if ((meta == null) || !meta.contains(key)) {
				merged.add(key, master.get(key));
				System.out.println("  From master: key=" + key + ", value=" + master.get(key));
			} else {
				merged.add(key, slave.get(key));
				System.out.println("  From  slave: key=" + key + ", value=" + master.get(key));
			}
		}
		return merged.build();
	}
}
