window.app = new Vue({
	el: '#app',
	data: function () {
		return {
			// Login dialog data
			uname: "",
			nameState: null,
			pwd: "",
			pwdState: null,
			// Flags for showing a wait cursor on different cards
			initializing: true,
			loading: {
				branchName: false,
				createBranch: false,
				diffFlows: false,
				diffui: false,
				updbranch: false,
				delbranch: false,
				dirty: false,
			},
			// Flags for showing message boxes of different cards
			retrieveResult: {
				branchName: "",
				createBranch: "",
				diffFlows: "",
				diffhtml: "",
				diffjs: "",
				diffcss: "",
				updbranch: "",
				delbranch: "",
				dirty: ""
			},
			hasChanges: 0,
			committing: false,
			commitMsg: "",
			commitResult: "",
			// background color for message box: danger: Orange, success: green
			errmsg: "danger",
			uptodate: false,	// flag set when latest trunk revision is already merged
			changedNodes: [],   // table of nodes in flows.json that have changed in the trunk
			nodeChangedFields: [
				{key: "tabName", label: "Tab"},
				{key: "nodeName", label: "Node"},
				{key: "nodeContent.type", label: "Type"},
				{key: "whatChanged", label: "what changed?"},
				{key: "show_details", label: "Code"}
			],
			addedNodes: [],		// tabel of nodes in flows.json that appear only in the trunk
			nodeAddedFields: [
				{key: "add", label: "Add?"},
				{key: "tabName", label: "Tab"},
				{key: "nodeName", label: "Node"},
				{key: "nodeContent.type", label: "Type"},
				{key: "show_details", label: "Code"}
			],
			err400: false, // flag that signals the HTTP 400 response
			// Collapse support data
			collapse: false,
			collapse_col_text: "<",
			collapse_col_tooltip: "Close left column",
			mainWidth: "9",
			// show/hide flags of sections
			showMergeFlows: false,
			showMergeCode: false,
			showMergeUi: false,
			hasUI: false,
			// Other data
			branchName: "",       // Stores the name of the current branch
			hasChanges: 0,        // Determine which files are dirty in the WC: 0=flows
			newBranchName: "",    // new branch name for Create New Branch
			branch:"remove",      // checkbox "Remove previous branch" data
			uiType: "html",       // 'html', 'js' or 'css'
			ui: ["",""],          // ui[0]=branch text, ui[1]=trunk text of UI data
			html: "",             // merged html
			js: "",               // merged js
			css: "",              // merged css
			currow: {},			  // .curhtml=branch code, .prvhtml=trunk code
			curix: -1,			  // index of selected row in function code table
			curcolor: "primary",  /// color of the "Merge"-button
			merged: [],           // flags if text is merged ([0]=flow, [1]=html, [2]=js, [3]=css)
			selected: [],         // List of branches that must be removed
			myBranches:[]         // List of branches that user can remove
		};
	},
	computed: {
	},
	methods: {
		/*
		* Page refresh
		*/
		refresh() {
			window.location.reload();
		},
		/*
		* Collapse the left column. Used by navbar-button id='collapse_button'.
		*/
		collapse_column() {
			this.collapse = !this.collapse;
			this.mainWidth = this.collapse ? "12" : "9";
			this.collapse_col_text = this.collapse ? ">" : "<";
			this.collapse_col_tooltip = this.collapse ? "Open left column" : "Close left column";
		},
		/*
		* Determines whether the Update section must be disabled
		*/
		disableUpdate() {
			this.merged[0] = true;
			for (var row of this.changedNodes) {
				this.merged[0] = this.merged[0] && row.merged;
			}
			var tmp = this.uptodate || !this.merged[0];
			if (this.hasUI) {
				tmp = tmp || !(this.merged[1] && this.merged[2] && this.merged[3]);
			}
			return tmp; 
		},
		/*
		* Determines whether the Remove section must be disabled
		*/
		disableRemove() {
			return (this.myBranches.length === 1 && this.myBranches[0] === this.branchName) || this.myBranches.length === 0;
		},
		/*
		* Retrieve a cookie with the give name
		*/
		getCookie(cname) {
			let name = cname + "=";
			let decodedCookie = decodeURIComponent(document.cookie);
			let ca = decodedCookie.split(';');
			for(let i = 0; i <ca.length; i++) {
				let c = ca[i];
				while (c.charAt(0) == ' ') {
					c = c.substring(1);
				}
				if (c.indexOf(name) == 0) {
					return c.substring(name.length, c.length);
				}
			}
			return "";
		},
		/*
		* Storeve a cookie with the give name, value and expiration days
		*/
		setCookie(cname, cvalue, exdays) {
			const d = new Date();
			d.setTime(d.getTime() + (exdays*24*60*60*1000));
			let expires = "expires="+ d.toUTCString();
			document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
		},
		/*
		* Check if there are cookies stored. If not, show dialog
		*/
		checkCookies() {
			this.uname = this.getCookie("SvnUser");
			this.pwd = this.getCookie("SvnPwd");
			if (this.uname === "" || this.pwd === "") {
				this.$refs['login'].show();
//			} else {
//				this.initializing = false;
			}
		},
		/*
		* Handle the Login dialog OK-button press
		*/
		handleLoginOk(bvModalEvt) {
			// Prevent modal from closing
			bvModalEvt.preventDefault();
			this.handleLoginSubmit();
		},
		/*
		* Store the cookie values from the Login dialog
		*/
		handleLoginSubmit() {
			const valid = true;
			if (this.uname === "") {
				this.nameState = false;
				valid = false;
			}
			if (this.pwd === "") {
				this.pwdState = false;
				valid = false;
			}
			if (!valid)	return;
			// Hide the modal manually
			this.$nextTick(() => {
				this.$bvModal.hide('login');
			});
			this.setCookie("SvnUser", this.uname, 365);
			this.setCookie("SvnPwd", this.pwd, 365);
//			this.initializing = false;
		},
		/*
		* Handle the "Reset credentials"-button press
		*/
		resetCredentials() {
			this.$refs['login'].show();
		},
		/*
		* Handle the "Merge"-button press
		*/
		handleMergeOk(bvModalEvt) {
			// Prevent modal from closing
			bvModalEvt.preventDefault();
			this.handleMergeSubmit();
		},
		/*
		* Handle the "Merge"-button press
		*/
		handleMergeSubmit() {
			// Hide the modal manually
			this.$nextTick(() => {
				this.$bvModal.hide('merge');
			});
			this.loading = true;
		},
		/*
		* Handle the "Done"-button press in the Merge page of flows
		*/
		doneMergingCode() {
			this.showMergeCode = false;
			this.showMergeFlows = true;
			this.showMergeUi = false;
			this.changedNodes[this.curix].curHtml = this.currow.curHtml;
			this.changedNodes[this.curix].nodeContent.func = this.currow.curHtml;
			this.changedNodes[this.curix].merged = true;
			this.$refs.cn.refresh(); // Refresh changedNodes table to show changes
		},
		/*
		* Handle the "Done"-button press in the Merge page of UI
		*/
		doneMergingUi() {
			this.merging = false;
			this.showMergeUi = false;
			this.showMergeFlows = false;
			this.showMergeCode = false;
			if (this.uiType === "html") {
				this.html = this.ui[0];
				this.merged[1] = true;
			} else if (this.uiType === "js") {
				this.js = this.ui[0];
				this.merged[2] = true;
			} else {
				this.css = this.ui[0];
				this.merged[3] = true;
			}
		},
		/*
		* Handle the "Merge"-button press in the function table
		*/
		onMergeCode(index, row) {
			this.showMergeCode = true;
			this.showMergeFlows = false;
			this.showMergeUi = false;
			this.curix = index;
			this.currow = row;
		},
		/*
		* Handle the scroll synchronization of the merge page
		*/
		sync() {
			this.$nextTick(function () {
				this.reset();
			});
		},
		reset() {
			var elems = this.$el.getElementsByClassName("syncscroll");
			var el1 = elems[0];
			var el2 = elems[1];
			// clearing existing listeners
			el1.removeEventListener('scroll', el1.syn, 0);
			el2.removeEventListener('scroll', el2.syn, 0);
			// setting-up the new listeners
			el1.eX = el1.eY = el2.eX = el2.eY = 0;
			this.syncScroll(el1, el2);
			this.syncScroll(el2, el1);
		},
		resetui(event) {
			var elems = this.$el.getElementsByClassName("syncscrollui");
			var el1 = elems[0];
			var el2 = elems[1];
			// clearing existing listeners
			el1.removeEventListener('scroll', el1.syn, 0);
			el2.removeEventListener('scroll', el2.syn, 0);
	        // setting-up the new listeners
            el1.eX = el1.eY = el2.eX = el2.eY = 0;
			this.syncScroll(el1, el2);
			this.syncScroll(el2, el1);
	    },
		syncScroll(el, otherEl) {
			el.addEventListener('scroll', el.syn = function () {
				var scrollX = el.scrollLeft;
				var scrollY = el.scrollTop;
				var xRate = scrollX / (el.scrollWidth - el.clientWidth);
				var yRate = scrollY / (el.scrollHeight - el.clientHeight);
				var updateX = scrollX != el.eX;
				var updateY = scrollY != el.eY;
				el.eX = scrollX;
				el.eY = scrollY;
				if (updateX && Math.round(otherEl.scrollLeft -
					(scrollX = otherEl.eX = Math.round(xRate * (otherEl.scrollWidth - otherEl.clientWidth))))
				) {otherEl.scrollLeft = scrollX;}
				if (updateY && Math.round(otherEl.scrollTop -
					(scrollY = otherEl.eY = Math.round(yRate * (otherEl.scrollHeight - otherEl.clientHeight))))
				) {otherEl.scrollTop = scrollY;}
			}, 0);
		},
		/*
		* Retrieve the changed data of the flow nodes
		*/
		diffFlows() {
			this.loading.diffFlows = true;
			this.errmsg = "success";
			axios.post("/changednodes")
			.then(response => {
				var f = response.data;
				if (f.added === undefined) {
					console.log(f.msg);
					this.retrieveResult.diffFlows = f.msg;
					if (String(f.msg).startsWith("Branch")) {
						this.merged[0] = true;
					}
				} else {
					console.log(f);
					this.addedNodes = f.added;
					this.changedNodes = f.changed;
					if (this.addedNodes.length === 0 && this.changedNodes.length === 0) {
						this.showMergeFlows = false;
						this.retrieveResult.diffFlows = "Flows are the same";
					} else {
						for (var i = 0; i < this.addedNodes.length; i++) {
							this.addedNodes[i].add = true;
						}
						for (var i = 0; i < this.changedNodes.length; i++) {
							this.changedNodes[i].merged = false;
						}
						this.showMergeFlows = true;
						this.retrieveResult.diffFlows = "";
					}
					this.uptodate = false;
				}
				this.loading.diffFlows = false;
//				this.initializing = false;
			})
			.catch(error => {
				console.log(error.message);
				this.retrieveResult.diffFlows = error.message;
				this.loading.diffFlows = false;
				this.errmsg = "danger";
//				this.initializing = false;
			});
		},
		/*
		* Retrieve the changed data of the UI files
		*/
		diffUI(type) {
			event.preventDefault();
			this.uiType = type;
			this.loading.diffui = true;
			this.errmsg = "success";
			axios.post("/changeduis/" + type)
			.then(response => {
			    var f = response.data;
			    console.log(f);
				if (!Array.isArray(f)) {
					console.log(f.msg);
					if (type === "html") {
						this.retrieveResult.diffhtml = f.msg;
					} else if (type === "js") {
						this.retrieveResult.diffjs = f.msg;
					} else {
						this.retrieveResult.diffcss = f.msg;
					}
				} else {
					if (f[0] === "") {
						console.log(type + "'s are the same");
						if (type === "html") {
							this.retrieveResult.diffhtml = type + "'s are the same";
							this.merged[1] = true;
						} else if (type === "js") {
							this.retrieveResult.diffjs = type + "'s are the same";
							this.merged[2] = true;
						} else {
							this.retrieveResult.diffcss = type + "'s are the same";
							this.merged[3] = true;
						}
					} else {
						this.ui = f;
						this.showMergeUi = true;
					}
				}
				this.loading.diffui = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult.diffhtml = "Please push button again.";
				} else {
			    	console.log(error.message);
					if (type === "html") {
						this.retrieveResult.diffhtml = error.message;
					} else if (type === "js") {
						this.retrieveResult.diffjs = error.message;
					} else {
						this.retrieveResult.diffcss = error.message;
					}
				}
				this.loading.diffui = false;
			});
		},
		/*
		* Retrieve the branch name in the WC
		*/
		getBranch() {
			this.retrieveResult.branchName = "";
			this.uptodate = false;
			this.loading.branchName= true;
			axios.post("/getbranch")
			.then(response => {
				var f = response.data;
				console.log(f);
				if (f.branch === "") {
					console.log(f.msg);
					this.retrieveResult.branchName = f.msg;
					this.errmsg = "danger";
				} else if (f.msg !== "") {
					console.log(f.msg);
					this.retrieveResult.branchName = f.msg;
					this.errmsg = "success";
					this.uptodate = true;
				}
				this.hasChanges = f.hasChanges;
				this.branchName = f.branch;
				this.loading.branchName = false;
				this.initializing = false;
			})
			.catch(error => {
				console.log(error.message);
				this.retrieveResult.branchName = error.message;
				this.errmsg = "danger";
				this.loading.branchName = false;
			});
		},
		/*
		* Retrieve the branches that the current user may remove
		*/
		getMyBranches() {
			this.retrieveResult.rem = "";
			this.loading.delbranch = true;
			this.errmsg = "success";
			this.myBranches = [];
			axios.post("/getbranches")
			.then(response => {
				for (var i = 0; i < response.data.length; i++) {
					if (response.data[i] === this.branchName) {
						this.myBranches.push({text:response.data[i],disabled:true});
					} else {
						this.myBranches.push(response.data[i]);
					}
				}
				this.selected = [];
				this.loading.delbranch = false;
			})
			.catch(error => {
				console.log(error.message);
				this.retrieveResult.delbranch = error.message;
				this.errmsg = "danger";
				this.loading.delbranch = false;
			});
		},
		/*
		* Create a new branch with the given name
		*/
		createBranch() {
			this.loading.createBranch = true;
			this.retrieveResult.createBranch = "";
			this.errmsg = "success";
			var url = "/createbranch/" + this.newBranchName
			if (this.branch === "remove") {
				url += ":" + this.branchName;
			}
			axios.post(url)
			.then(response => {
				var f = response.data;
				if (String(f).startsWith("svn")) {
					console.log(f);
					this.retrieveResult.createBranch = f;
				} else {
					this.retrieveResult.createBranch = "";
					
					this.branchName = this.newBranchName;
				}
				this.loading.createBranch = false;
				this.refresh();
			})
			.catch(error => {
				console.log(error.message);
				this.retrieveResult.createBranch = error.message;
				this.errmsg = "danger";
				this.loading.createBranch = false;
			});
		},
		/*
		* Update the merged branch
		*/
		updateBranch() {
			this.loading.updbranch = true;
			this.retrieveResult.updbranch = "";
			this.errmsg = "success";
			this.mergedNodes = [];
			console.log(this.addedNodes)
			console.log(this.changedNodes)
			for (var n of this.addedNodes) {
				if (n.add) {
					this.mergedNodes.push({nodeId: n.nodeId, nodeContent: n.nodeContent });
				}
			}
			for (var n of this.changedNodes) {
				this.mergedNodes.push({ nodeId: n.nodeId, nodeContent: n.nodeContent });
			}
			console.log(this.mergedNodes);
			axios.post("/updatebranch", {branch: this.branchName, nodes: this.mergedNodes, html: this.html, js: this.js, css: this.css})
			.then(response => {
				var f = response.data;
				if (String(f).startsWith("svn")) {
					console.log(f);
					this.retrieveResult.updbranch = f;
					this.errmsg = "danger";
					this.loading.updbranch = false;
				} else {
//					this.refresh();
				}
			})
			.catch(error => {
				console.log(error.message);
				this.retrieveResult.updbranch = error.message;
				this.errmsg = "danger";
				this.loading.updbranch = false;
			});
		},
		/*
		* Retrieve again the branches that the current user may remove
		*/
		refreshList() {
			this.getMyBranches();
			this.selected = [];
		},
		/*
		* Remove the selected branches
		*/
		removeBranches() {
			this.errmsg = "success";
			this.loading.rembranch = true;
			axios.post("/removebranch", this.selected)
			.then(response => {
				var f = response.data;
				if (String(f).startsWith("svn")) {
					console.log(f);
					this.retrieveResult.rembranch = f;
					this.errmsg = "danger";
				}
				this.loading.rembranch = false;
				this.getMyBranches();
			})
			.catch(error => {
				console.log(error.message);
				this.retrieveResult.rembranch = error.message;
				this.errmsg = "danger";
				this.loading.rembranch = false;
			});
		},
		onCommit(event) {
			this.commitResult = "";
			event.preventDefault();
			if (this.commitMsg !== "") {
				this.committing = true;
				axios.post("/commit/" + this.commitMsg, {})
				.then(response => {
					if (String(response.data).startsWith("svn")) {
						this.commitResult = response.data;
						this.alertType = "danger";
					} else if (Number(response.data) < 0) {
				    	this.commitResult = "Nothing to commit.";
						this.alertType = "success";
					} else {
						this.commitResult = "Committed in revision " + response.data + ".\nRefresh the page to perform the review";
						this.alertType = "success";
					}
					this.commitMsg = "";
					this.committing = false;
					this.curTrunkRev = response.data;
				})
				.catch(error => {
					if (error.response.status === 400) {
						this.commitResult = "Please push 'Commit' again."
					} else {
				    	console.log(error.message);
					}
					this.committing = false;
				});
			} else {
				this.commitResult = "Please give a commit reason.";
			}
		},
	},
	/*
	* Startup actions
	*/
	mounted: function () {
		this.checkCookies();
		this.sync();
		this.showMergeCode = false;
		this.showMergeUi = false;
		this.merged = [false, false, false, false];
		this.getBranch();
	}
})
