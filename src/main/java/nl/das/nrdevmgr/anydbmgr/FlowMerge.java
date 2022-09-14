/**
 *******************************************************************************************
 **
 **  @filename       FlowMerge.java
 **  @brief
 **
 **  @copyright      (c) Core|Vision B.V.,
 **                  Cereslaan 10b,
 **                  5384 VT  Heesch,
 **                  The Netherlands,
 **                  All Rights Reserved
 **
 **  @author         tom
 **  @svnversion     $Date: 2021-11-22 09:26:28 +0100 (Mon, 22 Nov 2021) $
 **                  $Revision: 49776 $
 **
 *******************************************************************************************
 */

package nl.das.nrdevmgr.anydbmgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * @author tom
 *
 */
public class FlowMerge {

	public String merge(String liveflow, String devflow) {
		JsonArray devnodes = new JsonParser().parse(devflow).getAsJsonArray();
		List<String> order = new ArrayList<>();
		Map<String, JsonObject> devnodemap = new HashMap<>();
		for (JsonElement jo : devnodes) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject n = (JsonObject)jo;
				String id = n.get("id").getAsString();
				devnodemap.put(id, n);
				order.add(id);
			}
		}
		JsonArray livenodes = new JsonParser().parse(liveflow).getAsJsonArray();
		Map<String, JsonObject> livenodemap = new HashMap<>();
		for (JsonElement jo : livenodes) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject n = (JsonObject)jo;
				String id = n.get("id").getAsString();
				livenodemap.put(id, n);
			}
		}
		for (String id : devnodemap.keySet()) {
			JsonObject dn = devnodemap.get(id);
			if (dn.get("type").getAsString().equalsIgnoreCase("mqtt-broker")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("mqtt-broker not found in live");
				} else {
					String[] fields = {"broker","port","clientid"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("MySQLdatabase")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("MySQLdatabase not found in live");
				} else {
					String[] fields = {"host","port","db"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("tcp in")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'tcp in' not found in live");
				} else {
					String[] fields = {"host","port","server"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("tcp out")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'tcp out' not found in live");
				} else {
					String[] fields = {"host","port","beserver"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("udp out")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'udp out' not found in live");
				} else {
					String[] fields = {"addr","port","outport"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("udp in")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'udp in' not found in live");
				} else {
					String[] fields = {"port"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("http request")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'http request' not found in live");
				} else {
					String[] fields = {"url"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.get("type").getAsString().equalsIgnoreCase("http in")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'http in' not found in live");
				} else {
					String[] fields = {"url"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			}
		}

		JsonArray newlive = new JsonArray();
		for (String id : order) {
			newlive.add(devnodemap.get(id));
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(newlive);
	}

	private void copy(String[] fields, JsonObject from, JsonObject to) {
		for (String f : fields) {
			to.addProperty(f, from.get(f).getAsString());
		}
	}

}
