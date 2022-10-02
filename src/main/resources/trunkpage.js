window.app = new Vue({
	el: '#app',
	data: function() {
		return {
			uname: "",
			nameState: null,
			pwd: "",
			pwdState: null,
			initializing: true,
			loading: {
				selbranch: false,
				combtflows: false,
				combthtml: false,
				combtjs: false,
				combtcss: false,
				comtflows: false,
				comthtml: false,
				comtjs: false,
				comtcss: false,
				comtsql: false,
				comvsql: false,
				compsql: false
			},
			retrieveResult: {
				selbranch: "",
				combtflows: "",
				combthtml: "",
				combtjs: "",
				combtcss: "",
				comtflows: "",
				comthtml: "",
				comtjs: "",
				comtcss: "",
				comtsql: "",
				comvsql: "",
				compsql: ""
			},
			committing: false,
			merging: false,
			hasChanges: true,
			branchrevno: "",
			trunkrevno: "",
			branchhuirevno: "",
			trunkhuirevno: "",
			branchjuirevno: "",
			trunkjuirevno: "",
			branchcuirevno: "",
			trunkcuirevno: "",
			currevno: "",
			prvrevno: "",
			curhuirevno: "",
			prvhuirevno: "",
			curjuirevno: "",
			prvjuirevno: "",
			curcuirevno: "",
			prvcuirevno: "",
			curtsqlrevno: "",
			prvtsqlrevno: "",
			curvsqlrevno: "",
			prvvsqlrevno: "",
			curpsqlrevno: "",
			prvpsqlrevno: "",
			commitResult: "",
			mergeResult: "",
			revOptions: [],
			revhuiOptions: [],
			revjuiOptions: [],
			revcuiOptions: [],
			revtsqlOptions: [],
			revvsqlOptions: [],
			revpsqlOptions: [],
			envOptions: [
			],
			nodeFields: [
				{key: "tabName", label: "Tab"},
				{key: "nodeName", label: "Node"},
				{key: "nodeContent.type", label: "Type"},
				{key: "whatChanged", label: "what changed?"},
				{key: "show_details", label: "Code"}
			],
			addedNodes: [],
			changedNodes: [],
			removedNodes: [],
			uitype: "",
			ui: ["",""],
			noUI: false,
			sqltype: "",
			sqlresult: ["",""],
		    names: {},
			commitMsg: "",
			alertType: "success",
			envSelected: "",
			err400: false,
			show_diff: false,
			show_ui: false,
			show_ui_result: false,
			show_nr: false,
			show_sql: false,
			show_sql_result: false,
			collapse: true,
			collapse_col_state: true,
			collapse_col_text: "<",
			collapse_col_tooltip: "Close left column",
			mainWidth: "12",
			diffHead1: "Branch",
			diffHead2: "Trunk",
			selBranch: "Select branch",
			curTrunkRev: 0
		};
	},
	computed: {
	},
	methods: {
		collapse_column() {
			this.collapse = !this.collapse;
			this.mainWidth = this.collapse ? "12" : "9";
			this.collapse_col_text = this.collapse ? ">" : "<";
			this.collapse_col_tooltip = this.collapse ? "Open left column" : "Close left column";
		},
		setCommitMessage() {
			this.commitMsg = "Merged branch " + this.envSelected;
		},
		diffFlows() {
			event.preventDefault();
			this.show_ui = false;
			this.show_diff = true;
			this.show_nr = true;
			this.retrieveResult.dirty = "";
			this.loading.dirty = true;
			axios.post("/changedflows")
			.then(response => {
				this.retrieveResult.dirty = "";
			    var f = response.data;
				if (String(f).startsWith("svn")) {
					this.retrieveResult.dirty = f;
				} else {
					this.addedNodes = f.added;
					this.changedNodes = f.changed;
					this.removedNodes = f.removed;
				}
				this.loading.dirty = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult.dirty = "Please push button again.";
				} else {
			    	console.log(error.message);
				}
				this.loading.dirty = false;
			});
		},
		diffUI() {
			event.preventDefault();
			this.retrieveResult6 = "";
			this.loading.dirty = true;
			this.show_ui = true;
			this.show_diff = false;
			this.show_nr = true;
			axios.post("/changeduis")
			.then(response => {
				this.retrieveResult.dirty = "";
			    var f = response.data;
				this.html = f.html;
				this.js = f.js;
				this.css = f.css;
				this.resetui({target: {innerText: "HTML"}})
				this.loading.dirty = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult.dirty = "Please push button again.";
				} else {
			    	console.log(error.message);
				}
				this.loading.dirty = false;
			});
		},
		onSubmitBranch(event) {
			event.preventDefault();
			this.loading.selbranch = true;
			this.retrieveResult.selbranch = "";
			if (this.envSelected === "sql") {
				this.show_diff = false;
				this.show_ui = false;
				this.show_ui_result = false;
				this.show_nr = false;
				this.show_sql_result = false;
				this.loading.selbranch = true;
				axios.post('/sqlrevisions')
				.then(response => {
					var f = response.data;
					if (String(f).startsWith("svn")) {
						console.log(f);
						this.retrieveResult.selbranch = f;
					} else {
						if (f.tableSql.length === 0) {
							this.retrieveResult.selbranch = "No SQL scripts found.";
							this.show_sql = false;
							this.noUI = true;
						} else {
							this.show_sql = true;
						    this.revtsqlOptions = f.tableSql;
						    this.revvsqlOptions = f.viewSql;
						    this.revpsqlOptions = f.procSql;
							this.retrieveResult.selbranch = "";
						}
					}
					this.loading.selbranch = false;
				})
				.catch(error => {
					console.log(error);
					if (error.response.status === 400) {
						this.retrieveResult.selbranch = "Please push 'Select' again.";
						console.log("Push again");
					} else {
						console.log(error);
					}
					this.loading.selbranch = false;
				});
			} else {
				this.show_nr = true;
				this.show_sql = false;
				this.show_sql_result = false;
				this.show_ui = false;
				this.show_ui_result = false;
				this.show_diff = false;
				this.addedFunctions = [],
				this.changedFunctions = [],
				this.removedFunctions = [],
				this.loading.selbranch = true;
				this.err400 = true;
				axios.post('/revisions/' + this.envSelected)
				.then(response => {
					var f = response.data;
					if (String(f).startsWith("svn")) {
						console.log(f);
						this.retrieveResult.selbranch = f;
					} else {
					    this.revOptions = f.trunkFlows;
					    if (f.trunkUiHtml !== 0) {
					    	this.noUI = true;
					    }
					    this.revhuiOptions = f.trunkUiHtml;
					    this.revjuiOptions = f.trunkUiJs;
					    this.revcuiOptions = f.trunkUiCss;
					    this.revtsqlOptions = f.tableSql;
					    this.revvsqlOptions = f.viewSql;
					    this.revpsqlOptions = f.procSql;
					    this.branchrevno = f.branchtrunkFlows[0];
						this.trunkrevno = f.branchtrunkFlows[1];
					    this.branchhuirevno = f.branchtrunkUiHtml[0];
						this.trunkhuirevno = f.branchtrunkUiHtml[1];
					    this.branchjuirevno = f.branchtrunkUiJs[0];
						this.trunkjuirevno = f.branchtrunkUiJs[1];
					    this.branchcuirevno = f.branchtrunkUiCss[0];
						this.trunkcuirevno = f.branchtrunkUiCss[1];
						this.retrieveResult.selbranch = f.message;
					}
					this.loading.selbranch = false;
				})
				.catch(error => {
					console.log(error);
					if (error.response.status === 400) {
						this.retrieveResult.selbranch = "Please push 'Select' again.";
						console.log("Push again");
					} else {
						console.log(error);
					}
					this.loading.selbranch = false;
				});
			}
		},
		onSubmitBranchTrunkFlows(event) {
			event.preventDefault();
			this.show_ui = false;
			this.retrieveResult.combtflows = "";
			this.loading.combtflows = true;
			this.diffHead1 = "Branch"
			this.diffHead2 = "Trunk"
			axios.post('/branchtrunkflows', {
			    revNo1: this.branchrevno,
			    revNo2: this.trunkrevno
			})
			.then(response => {
				this.retrieveResult.combtflows = "";
			    var f = response.data;
				if (String(f).startsWith("svn")) {
					this.retrieveResult.combtflows = f;
				} else {
					if (f.added.length === 0 && f.changed.length === 0 && f.removed.length === 0) {
						this.retrieveResult.combtflows = "Flows are the same";
						this.show_diff = false;
					} else {
						this.addedNodes = f.added;
						this.changedNodes = f.changed;
						this.removedNodes = f.removed;
						this.show_diff = true;
					}
				}
				this.loading.combtflows = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult.combtflows = "Please push 'Retrieve' again.";
				} else if (error.response.status === 500) {
					this.retrieveResult.combtflows = "Please enter both revisions.";
				} else {
			    	console.log(error.message);
				}
				this.loading.combtflows = false;
			});
		},
		onSubmitBranchTrunkUiHtml(event) {
			event.preventDefault();
			this.retrieveResult.combthtml = "";
			this.loading.combthtml = true;
			this.show_diff = false;
			this.diffHead1 = "Branch"
			this.diffHead2 = "Trunk"
			axios.post("/branchtrunkuis/html", {
			    revNo1: this.branchhuirevno,
			    revNo2: this.trunkhuirevno
			})
			.then(response => {
				this.retrieveResult.combthtml = "";
			    var f = response.data;
				if (String(f).startsWith("svn")) {
					console.log(f);
					this.retrieveResult.combthtml = f;
				} else {
					if (f[0] === "" && f[1] === "") {
						this.retrieveResult.combthtml = "HTML files are the same";
						this.show_ui = false;
					} else {
						this.uitype = "HTML file differences";
						this.ui = f;
						this.syncui();
						this.show_ui = true;
						this.show_ui_result = true;
					}
				}
				this.loading.combthtml = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult.combthtml = "Please push 'Retrieve' again.";
				} else if (error.response.status === 500) {
					this.retrieveResult.combthtml = "Please enter both revisions.";
				} else {
			    	console.log(error.message);
				}
				this.loading.combthtml = false;
			});
		},
		onSubmitBranchTrunkUiJs(event) {
			event.preventDefault();
			this.retrieveResult.combtjs = "";
			this.loading.combtjs = true;
			this.show_diff = false;
			this.show_ui = true;
			this.diffHead1 = "Branch"
			this.diffHead2 = "Trunk"
			axios.post("/branchtrunkuis/js", {
			    revNo1: this.branchjuirevno,
			    revNo2: this.trunkjuirevno
			})
			.then(response => {
				this.retrieveResult.combtjs = "";
			    var f = response.data;
				if (String(f).startsWith("svn")) {
					console.log(f);
					this.retrieveResult.combtjs = f;
				} else {
					if (f[0] === "" && f[1] === "") {
						this.retrieveResult.combtjs = "Javascript files are the same";
						this.show_ui = false;
					} else {
						this.uitype = "Javascript file differences";
						this.ui = f;
						this.syncui();
						this.show_ui = true;
						this.show_ui_result = true;
					}
				}
				this.loading.combtjs = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult.combtjs = "Please push 'Retrieve' again.";
				} else if (error.response.status === 500) {
					this.retrieveResult.combtjs = "Please enter both revisions.";
				} else {
			    	console.log(error.message);
				}
				this.loading.combtjs = false;
			});
		},
		onSubmitBranchTrunkUiCss(event) {
			event.preventDefault();
			this.retrieveResult.combtcss = "";
			this.loading.combtcss = true;
			this.show_diff = false;
			this.show_ui = true;
			this.diffHead1 = "Branch"
			this.diffHead2 = "Trunk"
			axios.post("/branchtrunkuis/css", {
			    revNo1: this.branchcuirevno,
			    revNo2: this.trunkcuirevno
			})
			.then(response => {
				this.retrieveResult.combtcss = "";
			    var f = response.data;
				if (String(f).startsWith("svn")) {
					console.log(f);
					this.retrieveResult.combtcss = f;
				} else {
					if (f[0] === "" && f[1] === "") {
						this.retrieveResult.combtcss = "CSS files are the same";
						this.show_ui = false;
					} else {
						this.uitype = "CSS file differences";
						this.ui = f;
						this.syncui();
						this.show_ui = true;
						this.show_ui_result = true;
					}
				}
				this.loading.combtcss = false;
			})
			.catch(error => {
				if (error.response.status === 400) {
					this.retrieveResult13 = "Please push 'Retrieve' again.";
				} else if (error.response.status === 500) {
					this.retrieveResult13 = "Please enter both revisions.";
				} else {
			    	console.log(error.message);
				}
				this.loading13 = false;
			});
		},
		onSubmitTrunkFlows(event) {
			event.preventDefault();
			if (this.currevno !== "" && this.prvrevno !== "") {
				this.show_ui = false;
				this.retrieveResult.comtflows = "";
				this.loading.comtflows = true;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/flows", {
				    revNo1: this.currevno,
				    revNo2: this.prvrevno
				})
				.then(response => {
					this.retrieveResult.comtflows = "";
				    var f = response.data;
					if (String(f).startsWith("svn")) {
						this.retrieveResult.comtflows = f;
					} else {
						if (f.added.length === 0 && f.changed.length === 0 && f.removed.length === 0) {
							this.retrieveResult.comtflows = "Flows are the same";
							this.show_diff = false;
						} else {
							this.addedNodess = f.added;
							this.changedNodes = f.changed;
							this.removedNodes = f.removed;
							this.show_diff = true;
						}
					}
					this.loading2 = false;
				})
				.catch(error => {
					if (error.response.status === 400) {
						this.retrieveResult.comtflows = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.comtflows = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading.comtflows = false;
				});
			} else {
				this.retrieveResult.comtflows = "Please enter both revisions.";
			}
		},
		onSubmitTrunkUiHtml(event) {
			event.preventDefault();
			if (this.curhuirevno !== "" && this.prvhuirevno !== "") {
				this.retrieveResult.comthtml = "";
				this.loading.comthtml = true;
				this.show_diff = false;
				this.show_ui = false;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/uis/html", {
				    revNo1: this.curhuirevno,
				    revNo2: this.prvhuirevno
				})
				.then(response => {
					this.retrieveResult.comthtml = "";
				    var f = response.data;
					if (String(f).startsWith("svn")) {
						console.log(f);
						this.retrieveResult.comthtml = f;
					} else {
						if (f[0] === "" && f[1] === "") {
							this.retrieveResult.comthtml = "Html files are the same";
							this.show_ui_result = false;
						} else {
							this.uitype = "HTML file differences";
							this.ui = f;
							this.syncui();
							this.show_ui = true;
							this.show_ui_result = true;
						}
					}
					this.loading.comthtml = false;
				})
				.catch(error => {
				    console.log(error.message);
					if (error.response.status === 400) {
						this.retrieveResult.comthtml = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.comthtml = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading.comthtml = false;
				});
			} else {
				this.retrieveResult.comthtml = "Please enter both revisions.";
			}
		},
		onSubmitTrunkUiJs(event) {
			event.preventDefault();
			if (this.curjuirevno !== "" && this.prvjuirevno !== "") {
				this.retrieveResult.comtjs = "";
				this.loading.comtjs = true;
				this.show_diff = false;
				this.show_ui = false;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/uis/js", {
				    revNo1: this.curjuirevno,
				    revNo2: this.prvjuirevno
				})
				.then(response => {
					this.retrieveResult = "";
				    var f = response.data;
					if (String(f).startsWith("svn")) {
						console.log(f);
						this.retrieveResult.comtjs = f;
					} else {
						if (f[0] === "" && f[1] === "") {
							this.retrieveResult.comtjs = "Javascript files are the same";
							this.show_ui_result = false;
						} else {
							this.uitype = "Javascript file differences";
							this.ui = f;
							this.syncui();
							this.show_ui = true;
							this.show_ui_result = true;
						}
					}
					this.loading.comtjs = false;
				})
				.catch(error => {
				    console.log(error.message);
					if (error.response.status === 400) {
						this.retrieveResult.comtjs = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.comtjs = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading.comtjs = false;
				});
			} else {
				this.retrieveResult.comtjs = "Please enter both revisions.";
			}
		},
		onSubmitTrunkUiCss(event) {
			event.preventDefault();
			if (this.curcuirevno !== "" && this.prvcuirevno !== "") {
				this.retrieveResult.comtcss = "";
				this.loading.comtcss = true;
				this.show_diff = false;
				this.show_ui = false;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/uis/css", {
				    revNo1: this.curcuirevno,
				    revNo2: this.prvcuirevno
				})
				.then(response => {
					this.retrieveResult = "";
				    var f = response.data;
					if (String(f).startsWith("svn")) {
						console.log(f);
						this.retrieveResult.comtcss = f;
					} else {
						if (f[0] === "" && f[1] === "") {
							this.retrieveResult.comtcss = "CSS files are the same";
							this.show_ui_result = false;
						} else {
							this.uitype = "CSS file differences";
							this.ui = f;
							this.syncui();
							this.show_ui = true;
							this.show_ui_result = true;
						}
					}
					this.loading.comtcss = false;
				})
				.catch(error => {
					if (error.response.status === 400) {
						this.retrieveResult.comtcss = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.comtcss = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading.comtcss = false;
				});
			} else {
				this.retrieveResult.comtcss = "Please enter both revisions.";
			}
		},
		onSubmitTableSql(event) {
			event.preventDefault();
			if (this.curtsqlrevno !== "" && this.prvstqlrevno !== "") {
				this.retrieveResult.comtsql = "";
				this.loading.comtsql = true;
				this.show_diff = false;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/sqls/tables", {
				    revNo1: this.curtsqlrevno,
				    revNo2: this.prvtsqlrevno
				})
				.then(response => {
					this.retrieveResult.comtsql = "";
				    var f = response.data;
				    console.log(f);
					if (String(f).startsWith("svn")) {
						this.retrieveResult.comtsql = f;
					} else {
						if (f[0] === "" && f[1] === "") {
							this.retrieveResult.comtsql = "Table scripts are the same";
							this.show_sql_result = false;
						} else {
							this.sqltype = "Tables sql script";
							this.sqlresult = f;
							this.show_sql_result = true;
							this.syncsql();
						}
					}
					this.loading.comtsql = false;
				})
				.catch(error => {
					if (error.response.status === 400) {
						this.retrieveResult.comtsql = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.comtsql = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading.comtsql = false;
				});
			} else {
				this.retrieveResult.comtsql = "Please enter both revisions.";
			}
		},
		onSubmitViewSql(event) {
			event.preventDefault();
			if (this.curvsqlrevno !== "" && this.prvvsqlrevno !== "") {
				this.retrieveResult.comvsql = "";
				this.loading8 = true;
				this.show_diff = false;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/sqls/views", {
				    revNo1: this.curvsqlrevno,
				    revNo2: this.prvvsqlrevno
				})
				.then(response => {
					this.retrieveResult = "";
				    var f = response.data;
					if (String(f).startsWith("svn")) {
						this.retrieveResult.comvsql = f;
					} else {
						if (f[0] === "" && f[1] === "") {
							this.retrieveResult.comvsql = "View scripts are the same";
							this.show_sql_result = false;
						} else {
							this.sqltype = "Views sql script";
							this.sqlresult = f;
							this.show_sql_result = true;
							this.syncsql();
						}
					}
					this.loading.comvsql = false;
				})
				.catch(error => {
					if (error.response.status === 400) {
						this.retrieveResult.comvsql = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.comvsql = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading.comvsql = false;
				});
			} else {
				this.retrieveResult.comvsql = "Please enter both revisions.";
			}
		},
		onSubmitProcSql(event) {
			event.preventDefault();
			if (this.curpsqlrevno !== "" && this.prvpsqlrevno !== "") {
				this.retrieveResult.compsql = "";
				this.loading.compsql = true;
				this.show_diff = false;
				this.diffHead1 = "Current"
				this.diffHead2 = "Previous"
				axios.post("/sqls/procedures", {
				    revNo1: this.curpsqlrevno,
				    revNo2: this.prvpsqlrevno
				})
				.then(response => {
					this.retrieveResult.compsql = "";
				    var f = response.data;
					if (String(f).startsWith("svn")) {
						this.retrieveResult.compsql = f;
					} else {
						if (f[0] === "" && f[1] === "") {
							this.retrieveResult.compsql = "Procedure scripts are the same";
							this.show_sql_result = false;
						} else {
							this.sqltype = "Procedures sql script";
							this.sqlresult = f;
							this.show_sql_result = true;
							this.syncsql();
						}
					}
					this.loading.compsql = false;
				})
				.catch(error => {
					if (error.response.status === 400) {
						this.retrieveResult.compsql = "Please push 'Retrieve' again.";
					} else if (error.response.status === 500) {
						this.retrieveResult.compsql = "Please enter both revisions.";
					} else {
				    	console.log(error.message);
					}
					this.loading9 = false;
				});
			} else {
				this.retrieveResult.compsql = "Please enter both revisions.";
			}
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
		onMergeAndCommit(event) {
			this.commitResult = "";
			event.preventDefault();
			if (this.commitMsg !== "") {
				this.committing = true;
				axios.post("/mergecommit/" + this.commitMsg, {
				    revNo1: this.branchrevno,
				    revNo2: this.branchhuirevno,
				    revNo3: this.branchjuirevno,
				    revNo4: this.branchcuirevno
				})
				.then(response => {
					if (String(response.data).startsWith("svn") || String(response.data).startsWith("Cannot")) {
						this.commitResult = response.data;
						this.alertType = "danger";
					} else if (Number(response.data) < 0) {
				    	this.commitResult = "Nothing to commit.";
						this.alertType = "success";
					} else if (Number(response.data) > 0) {
						this.commitResult = "Committed in revision " + response.data;
						this.alertType = "success";
					} else {
						this.commitResult = response.data;
						this.alertType = "success";
					}
					this.committing = false;
				})
				.catch(error => {
					console.log(error);
					if (error.response.status === 400) {
						this.commitResult = "Please push 'Commit' again."
					} else {
				    	console.log(error.message);
					}
					this.committing = false;
				});
				console.log("Merge&Commit sent");
			} else {
				this.commitResult = "Please give a commit reason.";
			}
		},
		sync(event) {
			this.$nextTick(function() {
				this.reset(event);
			}
		)},
		reset(event) {
			var elems = this.$el.getElementsByClassName("syncscroll");
			var el1 = elems[0];
			var el2 = elems[1];
	        // clearing existing listeners
			if (event.target.innerText === "Hide Code") {
				el1.removeEventListener('scroll', el1.syn, 0);
				el2.removeEventListener('scroll', el2.syn, 0);
		        // setting-up the new listeners
	            el1.eX = el1.eY = el2.eX = el2.eY = 0;
				this.syncScroll(el1, el2);
				this.syncScroll(el2, el1);
			}
	    },
		syncui(event) {
			this.$nextTick(function() {
				this.resetui(event);
			}
		)},
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
		syncsql(event) {
			this.$nextTick(function() {
				this.resetsql(event);
			}
		)},
		resetsql(event) {
			var elems = this.$el.getElementsByClassName("syncscrollsql");
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
            el.addEventListener('scroll', el.syn = function() {
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
                ) { otherEl.scrollLeft = scrollX; }
                if (updateY && Math.round(otherEl.scrollTop -
                        (scrollY = otherEl.eY = Math.round(yRate * (otherEl.scrollHeight - otherEl.clientHeight))))
                ) { otherEl.scrollTop = scrollY; }
            }, 0);
		},
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
		setCookie(cname, cvalue, exdays) {
			const d = new Date();
			d.setTime(d.getTime() + (exdays*24*60*60*1000));
			let expires = "expires="+ d.toUTCString();
			document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
		},
		checkCookies() {
			this.uname = this.getCookie("SvnUser");
			this.pwd = this.getCookie("SvnPwd");
			if (this.uname === "" || this.pwd === "") {
				this.$refs['login'].show();
			} else {
				this.collapse_column();
				this.loading = true;
				this.getBranches();
			}
		},
		handleLoginOk(bvModalEvt) {
			// Prevent modal from closing
			bvModalEvt.preventDefault();
			this.handleLoginSubmit();
		},
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
			this.collapse_col();
			this.loading = true;
			this.getBranches();
		},
		getBranches() {
			this.loading.selbranch = true;
			this.retrieveResult.selbranch = "";
			axios.post('/branches', {withCredentials: true})
			.then(response => {
				var f = response.data;
				console.log(f);
				if (String(f).startsWith("svn")) {
					this.retrieveResult.selbranch = f;
				} else {
					this.hasChanges = response.data.hasChanges;
					for (env of response.data.branches) {
				    	this.envOptions.push({value: env, text: "Branch " + env});
					}
				    this.envOptions.push({value: "sql", text: "SQL scripts"});
					this.selBranch = "Select branch (current trunk rev# is " + response.data.curTrunkRev + ")";
				}
				this.loading.selbranch = false;
				this.initializing = false;
			})
			.catch(error => {
		    	console.log(error.message);
				this.loading.selbranch = false;
				this.initializing = false;
			});
		}
	},
	mounted: function() {
		this.initializing = true;
		this.checkCookies();
	}
})
