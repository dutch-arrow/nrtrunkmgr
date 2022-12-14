package nl.das.nrtrunkmgr.model;

import javax.json.JsonObject;

/**
 *******************************************************************************************
 **
 **  @filename       FlowFunction.java
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

/**
 * @author tom
 *
 */
public class FlowNode implements Comparable<FlowNode> {
	private String tabId;
	private String tabName;
	private String nodeId;
	private String nodeName;
	private String whatChanged;
	private String curHtml;
	private String prvHtml;
	private JsonObject nodeContent;


    public FlowNode(String tabId, String tab, String nodeId, String name, String whatChanged, JsonObject content) {
    	this.tabId = tabId;
    	this.tabName = tab;
    	this.nodeId = nodeId;
    	this.nodeName = name;
       	this.whatChanged = whatChanged;
    	this.curHtml = "";
    	this.prvHtml = "";
    	this.nodeContent = content;
    }
	public FlowNode(String tabId, String tab, String nodeId, String name, String whatChanged, String curHtml, String prvHtml, JsonObject content) {
    	this.tabId = tabId;
    	this.tabName = tab;
    	this.nodeId = nodeId;
    	this.nodeName = name;
    	this.whatChanged = whatChanged;
    	this.curHtml = curHtml;
    	this.prvHtml = prvHtml;
    	this.nodeContent = content;
	}

	public String getTabId() {
		return this.tabId;
	}

	public void setTabId(String tabId) {
		this.tabId = tabId;
	}

	public String getTabName() {
		return this.tabName;
	}

	public void setTabName(String tabName) {
		this.tabName = tabName;
	}

	public String getNodeId() {
		return this.nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeName() {
		return this.nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getWhatChanged() {
		return this.whatChanged;
	}

	public void setWhatChanged(String whatChanged) {
		this.whatChanged = whatChanged;
	}

	public String getCurHtml() {
		return this.curHtml;
	}

	public void setCurHtml(String curHtml) {
		this.curHtml = curHtml;
	}

	public String getPrvHtml() {
		return this.prvHtml;
	}

	public void setPrvHtml(String prvHtml) {
		this.prvHtml = prvHtml;
	}

	public JsonObject getNodeContent() {
		return this.nodeContent;
	}

	public void setNodeContent(JsonObject nodeContent) {
		this.nodeContent = nodeContent;
	}

	@Override
	public int compareTo(FlowNode o) {
		return this.getTabName().compareTo(o.getTabName());
	}

}
