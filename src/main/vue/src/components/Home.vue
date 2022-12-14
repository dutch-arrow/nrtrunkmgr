<template>
  <div v-show="showPage">
    <div v-show="showSelection">
      <b-card title="Overview" style="margin: auto; width: fit-content">
        <b-overlay :show="loading" variant="light" opacity="0.6" rounded="sm"></b-overlay>
        <label>Choose item to review</label>
        <b-form @submit="onSubmitBranch">
          <b-container fluid class="ps-0">
            <b-row>
              <b-col cols="8">
                <b-form-select id="selenv" v-model="curBranchName" :options="branches" class="mr-auto"></b-form-select>
              </b-col>
              <b-col cols="4" class="mt-auto">
                <b-button style="width: 100%" type="submit" variant="primary">Select</b-button>
              </b-col>
            </b-row>
          </b-container>
        </b-form>
        <b-alert v-show="retrieveResult !== ''" :variant="errmsg" show>{{ retrieveResult }}</b-alert>
        <b-container fluid v-show="showOverview"  class="ps-0 pt-2">
          <b-row>
            <b-col>
              <label class="sr-only" for="remoteUrl">SVN location of the current trunk</label>
              <b-form-input id="remoteUrl" v-model="remoteUrl" class="border"></b-form-input>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <label class="sr-only mt-2" for="wcLocation">Working Copy location of current trunk</label>
              <b-form-input id="wcLocation" v-model="wcLocation" class="border"></b-form-input>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <label class="sr-only mt-2" for="currentBranchRev">Latest trunk Revision</label>
              <b-form-input id="currentTrunkRev" v-model="currentTrunkRev" class="border"></b-form-input>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <label class="sr-only mt-2" for="currentBranchRev">Latest branch Revision</label>
              <b-form-input id="currentBranchRev" v-model="currentBranchRev" class="border"></b-form-input>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <label class="sr-only mt-2" for="highestTrunkRevInBranch">Highest trunk revision in branch</label>
              <b-form-input id="highestTrunkRevInBranch" v-model="highestTrunkRevInBranch" class="border"></b-form-input>
            </b-col>
          </b-row>
          <b-row>
            <b-col>
              <b-container fluid style="width:500px;">
                <b-row>
                  <b-col cols="2">
                  </b-col>
                  <b-col md="4">
                    <label class="sr-only mt-2" for="latestBranchRev0">Rev nr Branch</label>
                  </b-col>
                  <b-col md="4">
                    <label class="sr-only mt-2" for="latestBranchRev0">Rev nr Trunk</label>
                  </b-col>
                  <b-col md="2">
                    <label class="sr-only mt-2" for="latestBranchRev0"></label>
                  </b-col>
                </b-row>
                <b-row>
                  <b-col cols="2">
                    <label class="sr-only mt-2 mr-2">flows.json</label>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestBranchRevs[0]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestTrunkRevs[0]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="2" class="ps-0 pe-0">
                    <b-button class="w-100" variant="primary" v-show="show_button[0]" v-on:click="flowDiff">Show</b-button>
                  </b-col>
                </b-row>
                <b-row class="mt-2">
                  <b-col cols="2">
                    <label class="sr-only mt-2 mr-2">index.html</label>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestBranchRevs[1]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestTrunkRevs[1]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="2" class="ps-0 pe-0">
                    <b-button class="w-100" variant="primary" v-show="show_button[1]" v-on:click="uiDiff('html')">Show</b-button>
                  </b-col>
                </b-row>
                <b-row class="mt-2">
                  <b-col cols="2">
                    <label class="sr-only mt-2 mr-2">index.js</label>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestBranchRevs[2]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestTrunkRevs[2]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="2" class="ps-0 pe-0">
                    <b-button class="w-100" variant="primary" v-show="show_button[2]" v-on:click="uiDiff('js')">Show</b-button>
                  </b-col>
                </b-row>
                <b-row class="mt-2">
                  <b-col cols="2">
                    <label class="sr-only mt-2 mr-2">index.css</label>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestBranchRevs[3]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="4">
                    <b-form-input v-model="latestTrunkRevs[3]" class="border"></b-form-input>
                  </b-col>
                  <b-col md="2" class="ps-0 pe-0">
                    <b-button class="w-100" variant="primary" v-show="show_button[3]" v-on:click="uiDiff('css')">Show</b-button>
                  </b-col>
                </b-row>
              </b-container>
            </b-col>
          </b-row>
          <b-row v-show="showMergeButton">
            <b-col cols="12">
              <b-button variant="primary" v-on:click="onMergeAndCommit" class="mt-2 w-100">Merge Branch into Trunk</b-button>
            </b-col>
          </b-row>
        </b-container>
        <b-overlay :show="committing" variant="light" opacity="0.6" rounded="sm"></b-overlay>
        <b-alert v-show="commitResult !== ''" :variant="errmsg" show>{{ commitResult }}</b-alert>
      </b-card>
    </div>
    <div v-show="showFlowDiff">
      <flow-difference :showFlowDiff="showFlowDiff"></flow-difference>
    </div>
    <div v-show="showUiDiff">
      <ui-difference :showUiDiff="showUiDiff"></ui-difference>
    </div>
  </div>
</template>

<script>
import FlowDifference from './FlowDifference.vue';
import UiDifference from './UiDifference.vue';
export default {
  name: "HomePage",
  props: ["showPage"],
  components: {
    FlowDifference,
    UiDifference,
  },
  data() {
    return {
      curTrunkInfo: {},
      curBranchName: "",
      wcLocation: "",
      currentBranchRev: 0,
      currentTrunkRev: 0,
      remoteUrl: "",
      branches: [],
      latestBranchRevs: [],
      latestTrunkRevs: [],
      show_button: [false,false,false,false],
      showMergeButton: false,
      showSelection: false,
      showOverview: false,
      showFlowDiff: false,
      flowsDiff: false,
      showUiDiff: false,
      uiDiff: false,
      highestTrunkRevInBranch: 0,
      loading: false,
      errmsg: "",
      retrieveResult: "",
      commitResult: "",
      committing: false,
    };
  },
  methods: {
    getBranches() {
      this.loading = true;
      this.retrieveResult = "";
      this.commitResult = "";
      this.axios
        .post("/api/getbranches")
        .then((response) => {
          var f = response.data;
          console.log(f);
          if (String(f).startsWith("svn")) {
            this.retrieveResult = f;
          } else {
            this.hasChanges = response.data.hasChanges;
            for (var brname of response.data.branches) {
              this.branches.push({ value: brname, text: "Branch " + brname });
            }
            this.branches.push({ value: "sql", text: "SQL scripts" });
          }
          this.loading = false;
        })
        .catch((error) => {
          console.log(error.message);
          this.retrieveResult = error.message;
          this.loading = false;
        });
    },
    onSubmitBranch() {
      this.loading = true;
      this.retrieveResult = "";
      this.commitResult = "";
      if (this.selBranch === "sql") {
        this.loading = true;
        this.axios
          .post("/api/sqlrevisions")
          .then((response) => {
            var f = response.data;
            if (String(f).startsWith("svn")) {
              console.log(f);
              this.retrieveResult = f;
            } else {
              if (f.tableSql.length === 0) {
                this.retrieveResult = "No SQL scripts found.";
              } else {
                this.show_sql = true;
                this.revtsqlOptions = f.tableSql;
                this.revvsqlOptions = f.viewSql;
                this.revpsqlOptions = f.procSql;
                this.retrieveResult = "";
              }
            }
            this.loading = false;
          })
          .catch((error) => {
            console.log(error);
            if (error.response.status === 400) {
              this.retrieveResult = "Please push 'Select' again.";
              console.log("Push again");
            } else {
              console.log(error);
            }
            this.loading = false;
          });
      } else {
        this.loading = true;
        this.err400 = true;
        this.axios.post("/api/getrevisions/" + this.curBranchName)
          .then((response) => {
            var f = response.data;
            if (String(f).startsWith("svn")) {
              console.log(f);
              this.retrieveResult = f;
            } else {
              this.revOptions = f.trunkFlows;
              if (f.trunkUiHtml !== 0) {
                this.noUI = true;
              }
              console.log(f);
              this.wcLocation = f.wcLocation;
              this.remoteUrl = f.remoteUrl;
              this.latestBranchRevs = f.latestBranchRevs;
              this.currentBranchRev = f.currentBranchRev;
              this.latestTrunkRevs = f.latestTrunkRevs;
              this.currentTrunkRev = f.currentTrunkRev;
              this.highestTrunkRevInBranch = f.highestTrunkRevInBranch;
              this.uptodate = f.uptodate;
              this.showSelection = true;
              this.showOverview = true;
              this.showFlowDiff = false;
              this.showUiDiff = false;
              this.showMergeButton = false;
              for (var i = 0; i < 4; i++) {
                this.show_button[i] = this.latestBranchRevs[i] != 0 && this.latestTrunkRevs[i] != 0 && this.latestBranchRevs[i] > this.latestTrunkRevs[i];
                this.showMergeButton |= this.show_button[i];
              }
              if (this.currentTrunkRev > this.currentBranchRev) {
                this.retrieveResult = "Branch is not up-to-date with Trunk.";
                this.errmsg = "danger";
              }
            }
            this.loading = false;
          })
          .catch((error) => {
            console.log(error);
            if (error.response.status === 400) {
              this.retrieveResult = "Please push 'Select' again.";
              console.log("Push again");
            } else {
              console.log(error);
            }
            this.loading = false;
            this.emitter.emit("FlowDifference", {brname: this.curBranchName, brev: this.latestBranchRevs[0], trev: this.latestTrunkRevs[0]});
            this.emitter.emit("UiDifference", {brname: this.curBranchName, type: 'html', brev: this.latestBranchRevs[1], trev: this.latestTrunkRevs[1]});
            this.emitter.emit("UiDifference", {brname: this.curBranchName, type: 'js', brev: this.latestBranchRevs[2], trev: this.latestTrunkRevs[2]});
            this.emitter.emit("UiDifference", {brname: this.curBranchName, type: 'css', brev: this.latestBranchRevs[3], trev: this.latestTrunkRevs[3]});
          });
      }
    },
    flowDiff() {
      this.showSelection = false;
      this.showOverview = false;
      this.showFlowDiff = true;
      this.showUiDiff = false;
    },
    uiDiff(type) {
      this.showSelection = false;
      this.showOverview = false;
      this.showFlowDiff = false;
      this.showUiDiff = true;
    },
    onMergeAndCommit() {
      this.commitResult = "";
      this.commitMsg = "Merged branch " + this.curBranchName;
      this.committing = true;
      this.axios.post("/api/mergecommit/" + this.commitMsg, {
          brName: this.curBranchName,
          revNo1: (this.show_button[0] ? this.latestBranchRevs[0] : 0),
          revNo2: (this.show_button[1] ? this.latestBranchRevs[1] : 0),
          revNo3: (this.show_button[2] ? this.latestBranchRevs[2] : 0),
          revNo4: (this.show_button[3] ? this.latestBranchRevs[3] : 0)
      })
      .then(response => {
        if (String(response.data).startsWith("svn") || String(response.data).startsWith("Cannot")) {
          this.commitResult = response.data;
          this.errmsg = "danger";
        } else if (Number(response.data) < 0) {
            this.commitResult = "Nothing to commit.";
          this.errmsg = "success";
        } else if (Number(response.data) > 0) {
          this.commitResult = "Committed in revision " + response.data;
          this.errmsg = "success";
        } else {
          this.commitResult = response.data;
          this.errmsg = "success";
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
    },
  },
  mounted() {
    this.emitter.on("GetBranches", () => {
      console.log("received GetBranches message");
      this.showSelection = true;
      this.showOverview = false;
      this.showFlowDiff = false;
      this.showUiDiff = false;
      this.getBranches();
    });
    this.emitter.on("GetSQL", () => {
      console.log("received GetSQL message");
//      this.getSql();
    });
    this.emitter.on("BackToOverview", () => {
      console.log("received BackToOverview message");
      this.showSelection = true;
      this.showOverview = true;
      this.showFlowDiff = false;
      this.showUiDiff = false;
    });
    this.emitter.on("FlowIsDifferent", (diff) => {
      this.flowsDiff = diff;
    });
    this.emitter.on("UiIsDifferent", (diff) => {
      this.uiDiff = diff;
    });
  },
};
</script>
<style></style>
