/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 14 Oct 2022.
 */


package nl.das.nrtrunkmgr.model;

/**
 *
 */
public class TrunkInfo {

	private int uptodate; // 0 if uptodate, > 0 dirty: bit 1=flow, bit 2=html, bit 3=js, bit 4=css
	private String wcLocation;
	private String remoteUrl;
	private long currentTrunkRev;
	private long latestTrunkRevs[];  // 0=flows.json, 1=index.html, 2=index.js, 3=index.css
	private String branchName;
	private long currentBranchRev;
	private long latestBranchRevs[]; // 0=flows.json, 1=index.html, 2=index.js, 3=index.css
	private long highestTrunkRevInBranch;

	public int getUptodate () {
		return this.uptodate;
	}
	public void setUptodate (int uptodate) {
		this.uptodate = uptodate;
	}
	public String getWcLocation () {
		return this.wcLocation;
	}
	public void setWcLocation (String wcLocation) {
		this.wcLocation = wcLocation;
	}
	public String getRemoteUrl () {
		return this.remoteUrl;
	}
	public void setRemoteUrl (String remoteUrl) {
		this.remoteUrl = remoteUrl;
	}
	public long getCurrentTrunkRev () {
		return this.currentTrunkRev;
	}
	public void setCurrentTrunkRev (long currentTrunkRev) {
		this.currentTrunkRev = currentTrunkRev;
	}
	public long[] getLatestBranchRevs () {
		return this.latestBranchRevs;
	}
	public void setLatestBranchRevs (long[] latestBranchRevs) {
		this.latestBranchRevs = latestBranchRevs;
	}
	public long[] getLatestTrunkRevs () {
		return this.latestTrunkRevs;
	}
	public void setLatestTrunkRevs (long[] latestTrunkRevs) {
		this.latestTrunkRevs = latestTrunkRevs;
	}
	public String getBranchName () {
		return this.branchName;
	}
	public void setBranchName (String branchName) {
		this.branchName = branchName;
	}
	public long getCurrentBranchRev () {
		return this.currentBranchRev;
	}
	public void setCurrentBranchRev (long currentBranchRev) {
		this.currentBranchRev = currentBranchRev;
	}
	public long getHighestTrunkRevInBranch () {
		return this.highestTrunkRevInBranch;
	}
	public void setHighestTrunkRevInBranch (long highestTrunkRevInBranch) {
		this.highestTrunkRevInBranch = highestTrunkRevInBranch;
	}

}
