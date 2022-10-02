/**
 *******************************************************************************************
 **
 **  @filename       Node.java
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


package nl.das.nrdevmgr.model;

import javax.json.JsonObject;

/**
 *
 */
public class Node {

	// GUID
	private String id;
	// type
	private String type;
	private JsonObject obj;

	public Node(String id, String type, JsonObject obj) {
		this.id = id;
		this.type = type;
		this.obj = obj;
	}
	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public JsonObject getJson() {
		return this.obj;
	}
	public void setJson(JsonObject json) {
		this.obj = json;
	}


}
