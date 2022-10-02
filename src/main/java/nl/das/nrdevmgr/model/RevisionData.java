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

package nl.das.nrdevmgr.model;

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
	private List<Long> branchtrunkFlows;
	private List<Long> branchtrunkUiHtml;
	private List<Long> branchtrunkUiJs;
	private List<Long> branchtrunkUiCss;
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
	public List<Long> getBranchtrunkFlows() {
		return this.branchtrunkFlows;
	}
	public void setBranchtrunkFlows(List<Long> branchtrunkFlows) {
		this.branchtrunkFlows = branchtrunkFlows;
	}
	public List<Long> getBranchtrunkUiHtml() {
		return this.branchtrunkUiHtml;
	}
	public void setBranchtrunkUiHtml(List<Long> branchtrunkUiHtml) {
		this.branchtrunkUiHtml = branchtrunkUiHtml;
	}
	public List<Long> getBranchtrunkUiJs() {
		return this.branchtrunkUiJs;
	}
	public void setBranchtrunkUiJs(List<Long> branchtrunkUiJs) {
		this.branchtrunkUiJs = branchtrunkUiJs;
	}
	public List<Long> getBranchtrunkUiCss() {
		return this.branchtrunkUiCss;
	}
	public void setBranchtrunkUiCss(List<Long> branchtrunkUiCss) {
		this.branchtrunkUiCss = branchtrunkUiCss;
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
