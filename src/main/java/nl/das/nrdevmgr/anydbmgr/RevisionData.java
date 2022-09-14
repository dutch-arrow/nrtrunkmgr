/**
 *******************************************************************************************
 **
 **  @filename       RevisionData.java
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

import java.util.List;

/**
 * @author tom
 *
 */
public class RevisionData {
	private List<Long> liveFlows;
	private List<Long> liveUiHtml;
	private List<Long> liveUiJs;
	private List<Long> liveUiCss;
	private List<Long> devliveFlows;
	private List<Long> devliveUiHtml;
	private List<Long> devliveUiJs;
	private List<Long> devliveUiCss;
	private List<Long> tableSql;
	private List<Long> viewSql;
	private List<Long> procSql;
	private String message = "";

	public List<Long> getLiveFlows() {
		return this.liveFlows;
	}
	public void setLiveFlows(List<Long> liveFlows) {
		this.liveFlows = liveFlows;
	}
	public List<Long> getLiveUiHtml() {
		return this.liveUiHtml;
	}
	public void setLiveUiHtml(List<Long> liveUiHtml) {
		this.liveUiHtml = liveUiHtml;
	}
	public List<Long> getLiveUiJs() {
		return this.liveUiJs;
	}
	public void setLiveUiJs(List<Long> liveUiJs) {
		this.liveUiJs = liveUiJs;
	}
	public List<Long> getLiveUiCss() {
		return this.liveUiCss;
	}
	public void setLiveUiCss(List<Long> liveUiCss) {
		this.liveUiCss = liveUiCss;
	}
	public List<Long> getDevliveFlows() {
		return this.devliveFlows;
	}
	public void setDevliveFlows(List<Long> devliveFlows) {
		this.devliveFlows = devliveFlows;
	}
	public List<Long> getDevliveUiHtml() {
		return this.devliveUiHtml;
	}
	public void setDevliveUiHtml(List<Long> devliveUiHtml) {
		this.devliveUiHtml = devliveUiHtml;
	}
	public List<Long> getDevliveUiJs() {
		return this.devliveUiJs;
	}
	public void setDevliveUiJs(List<Long> devliveUiJs) {
		this.devliveUiJs = devliveUiJs;
	}
	public List<Long> getDevliveUiCss() {
		return this.devliveUiCss;
	}
	public void setDevliveUiCss(List<Long> devliveUiCss) {
		this.devliveUiCss = devliveUiCss;
	}
	public List<Long> getTableSql() {
		return this.tableSql;
	}
	public void setTableSql(List<Long> sql) {
		this.tableSql = sql;
	}
	public List<Long> getViewSql() {
		return this.viewSql;
	}
	public void setViewSql(List<Long> viewSql) {
		this.viewSql = viewSql;
	}
	public List<Long> getProcSql() {
		return this.procSql;
	}
	public void setProcSql(List<Long> procSql) {
		this.procSql = procSql;
	}
	public String getMessage() {
		return this.message;
	}
	public void setMessage(String message) {
		this.message = message;
	}


}
