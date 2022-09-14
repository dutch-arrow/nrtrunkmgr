/**
 *******************************************************************************************
 **
 **  @filename       SvnRevisions.java
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


package nl.das.nrdevmgr.anydbmgr;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author tom
 *
 */
public class SvnRevisions {
    @SerializedName("revno1")
    @Expose
	long revNo1;
    @SerializedName("revno2")
    @Expose
	long revNo2;
    @SerializedName("revno3")
    @Expose
	long revNo3;
    @SerializedName("revno4")
    @Expose
	long revNo4;

	public long getRevNo1() {
		return this.revNo1;
	}
	public void setRevNo1(String revNo) {
		this.revNo1 = Integer.parseInt(revNo);
	}
	public long getRevNo2() {
		return this.revNo2;
	}
	public void setRevNo2(String revNo) {
		this.revNo2 = Integer.parseInt(revNo);
	}
	public long getRevNo3() {
		return this.revNo3;
	}
	public void setRevNo3(String revNo) {
		this.revNo3 = Integer.parseInt(revNo);
	}
	public long getRevNo4() {
		return this.revNo4;
	}
	public void setRevNo4(String revNo) {
		this.revNo4 = Integer.parseInt(revNo);
	}


}
