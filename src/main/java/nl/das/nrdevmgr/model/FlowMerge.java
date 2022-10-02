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

package nl.das.nrdevmgr.model;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;


/**
 * @author tom
 *
 */
public class FlowMerge {

	public String merge(String liveflow, String devflow) {
		JsonArray devnodes = Json.createReader(new StringReader(devflow)).readArray();
		List<String> order = new ArrayList<>();
		Map<String, JsonObject> devnodemap = new HashMap<>();
		for (JsonValue jo : devnodes) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject n = (JsonObject)jo;
				String id = n.getString("id");
				devnodemap.put(id, n);
				order.add(id);
			}
		}
		JsonArray livenodes = Json.createReader(new StringReader(liveflow)).readArray();
		Map<String, JsonObject> livenodemap = new HashMap<>();
		for (JsonValue jo : livenodes) {
			if (jo.getClass() == JsonObject.class) {
				JsonObject n = (JsonObject)jo;
				String id = n.getString("id");
				livenodemap.put(id, n);
			}
		}
		for (String id : devnodemap.keySet()) {
			JsonObject dn = devnodemap.get(id);
			if (dn.getString("type").equalsIgnoreCase("mqtt-broker")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("mqtt-broker not found in live");
				} else {
					String[] fields = {"broker","port","clientid"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("MySQLdatabase")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("MySQLdatabase not found in live");
				} else {
					String[] fields = {"host","port","db"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("tcp in")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'tcp in' not found in live");
				} else {
					String[] fields = {"host","port","server"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("tcp out")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'tcp out' not found in live");
				} else {
					String[] fields = {"host","port","beserver"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("udp out")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'udp out' not found in live");
				} else {
					String[] fields = {"addr","port","outport"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("udp in")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'udp in' not found in live");
				} else {
					String[] fields = {"port"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("http request")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'http request' not found in live");
				} else {
					String[] fields = {"url"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			} else if (dn.getString("type").equalsIgnoreCase("http in")) {
				JsonObject ln = livenodemap.get(id);
				if (ln == null) {
					System.out.println("'http in' not found in live");
				} else {
					String[] fields = {"url"};
					copy(fields, livenodemap.get(id), devnodemap.get(id));
				}
			}
		}

		JsonArrayBuilder newlive = Json.createArrayBuilder();
		for (String id : order) {
			newlive.add(devnodemap.get(id));
		}
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		return jsonb.toJson(newlive);
	}

	private void copy(String[] fields, JsonObject from, JsonObject to) {
		for (String f : fields) {
			to.put(f, from.get(f));
		}
	}

}
