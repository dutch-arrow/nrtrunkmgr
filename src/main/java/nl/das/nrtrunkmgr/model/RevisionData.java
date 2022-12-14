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

package nl.das.nrtrunkmgr.model;

import java.util.List;

/**
 * @author tom
 *
 */
public class RevisionData {
	private List<Long> trunkFlows;
	private List<Long> trunkUiHtml;
	private List<Long> trunkUiJs;
	private List<Long> trunkUiCss;
	private List<Long> branchFlows;
	private List<Long> branchUiHtml;
	private List<Long> branchUiJs;
	private List<Long> branchUiCss;
	private List<Long> tableSql;
	private List<Long> viewSql;
	private List<Long> procSql;
	private String message = "";

	public List<Long> getTrunkFlows() {
		return this.trunkFlows;
	}
	public void setTrunkFlows(List<Long> trunkFlows) {
		this.trunkFlows = trunkFlows;
	}
	public List<Long> getTrunkUiHtml() {
		return this.trunkUiHtml;
	}
	public void setTrunkUiHtml(List<Long> trunkUiHtml) {
		this.trunkUiHtml = trunkUiHtml;
	}
	public List<Long> getTrunkUiJs() {
		return this.trunkUiJs;
	}
	public void setTrunkUiJs(List<Long> trunkUiJs) {
		this.trunkUiJs = trunkUiJs;
	}
	public List<Long> getTrunkUiCss() {
		return this.trunkUiCss;
	}
	public void setTrunkUiCss(List<Long> trunkUiCss) {
		this.trunkUiCss = trunkUiCss;
	}
	public List<Long> getBranchFlows() {
		return this.branchFlows;
	}
	public void setBranchFlows(List<Long> branchFlows) {
		this.branchFlows = branchFlows;
	}
	public List<Long> getBranchUiHtml() {
		return this.branchUiHtml;
	}
	public void setBranchUiHtml(List<Long> branchUiHtml) {
		this.branchUiHtml = branchUiHtml;
	}
	public List<Long> getBranchUiJs() {
		return this.branchUiJs;
	}
	public void setBranchUiJs(List<Long> branchUiJs) {
		this.branchUiJs = branchUiJs;
	}
	public List<Long> getBranchUiCss() {
		return this.branchUiCss;
	}
	public void setBranchUiCss(List<Long> branchUiCss) {
		this.branchUiCss = branchUiCss;
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
