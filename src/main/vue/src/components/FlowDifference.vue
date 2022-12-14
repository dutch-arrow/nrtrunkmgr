<template>
  <b-card v-show="showFlowDiff" >
    <div class="card-title container-fluid">
      <div class="row">
        <div class="col-md-11">
          <h4>{{title}}</h4>
        </div>
        <div class="col-md-1" style="text-align:right;">
          <button class="btn-close" v-on:click="backToOverview"></button>
        </div>
      </div>
    </div>
    <b-alert v-show="retrieveResult !== ''" :variant="errmsg" show>{{ retrieveResult }}</b-alert>
    <b-overlay :show="loading" variant="light" opacity="0.6" rounded="sm" ></b-overlay>
    <div v-show="show_diff">
      <b-tabs card fill>
        <b-tab :title="addedTitle">
          <!-- 5-column table: tabName | nodeName | nodeContent.type | whatChanged | show_details -->
          <b-table small sticky-header="700px" :items="addedNodes" :fields="nodeFields" >
            <template>
              <col style="width: 25%" />
              <col />
              <col style="width: 10%" />
              <col style="width: 10%" />
            </template>
            <template #cell(show_details)="row">
              <b-button size="sm" @click="row.toggleDetails" class="mr-2" v-if="row.item.curHtml.length > 0" >
                {{ row.detailsShowing ? "Hide" : "Show" }} Code
              </b-button>
            </template>
            <template #row-details="row">
              <b-card>
                <b-row class="mb-2">
                  <b-col>
                    <pre class="code1" v-html="row.item.curHtml"></pre>
                  </b-col>
                </b-row>
              </b-card>
            </template>
          </b-table>
        </b-tab>
        <b-tab :title="removedTitle">
          <!-- 5-column table: tabName | nodeName | nodeContent.type | whatChanged | show_details -->
          <b-table small sticky-header="700px" :items="removedNodes" :fields="nodeFields" >
            <template>
              <col style="width: 25%" />
              <col />
              <col style="width: 10%" />
              <col style="width: 10%" />
            </template>
            <template #cell(show_details)="row">
              <b-button size="sm" @click="row.toggleDetails" class="mr-2" v-if="row.item.curHtml.length > 0" >
                {{ row.detailsShowing ? "Hide" : "Show" }} Code
              </b-button>
            </template>
            <template #row-details="row">
              <b-card>
                <b-row class="mb-2">
                  <b-col>
                    <pre class="code1" v-html="row.item.curHtml"></pre>
                  </b-col>
                </b-row>
              </b-card>
            </template>
          </b-table>
        </b-tab>
        <b-tab :title="changedTitle">
          <!-- 5-column table: tabName | nodeName | nodeContent.type | whatChanged | show_details -->
          <b-table small sticky-header="700px" :items="changedNodes" :fields="nodeFields">
            <template>
              <col style="width: 25%" />
              <col />
              <col style="width: 10%" />
              <col style="width: 10%" />
            </template>
            <template #cell(show_details)="row">
              <b-button size="sm" @click="row.toggleDetails" v-on:click="sync" class="mr-2" v-if=" row.item.curHtml.length > 0 || row.item.prvHtml.length > 0 " >
                {{ row.detailsShowing ? "Hide" : "Show" }} Code
              </b-button>
            </template>
            <template #row-details="row">
              <b-card>
                <b-row>
                  <b-col class="col-code">
                    <label style="font-weight: bolder">{{ diffHead1 }}</label>
                    <hr />
                    <div name="syncscroll1" class="syncscroll code2" v-html="row.item.curHtml" ></div>
                  </b-col>
                  <b-col class="col-code">
                    <label style="font-weight: bolder">{{ diffHead2 }}</label>
                    <hr />
                    <div name="syncscroll2" class="syncscroll code2" v-html="row.item.prvHtml" ></div>
                  </b-col>
                </b-row>
              </b-card>
            </template>
          </b-table>
        </b-tab>
      </b-tabs>
    </div>
  </b-card>
</template>

<script>
export default {
  name: "FlowDifference",
  props: ["showFlowDiff"],
  data() {
    return {
      loading: false,
      errmsg: "",
      retrieveResult: "",
      args: {brname:'', brev:0, trev: 0},
      show_diff: false,
      nodeFields: [
        {key: "tabName", label: "Tab"},
        {key: "nodeName", label: "Node"},
        {key: "nodeContent.type", label: "Type"},
        {key: "whatChanged", label: "what changed?"},
        {key: "show_details", label: "Code"}
      ],
      addedNodes: [],
      addedTitle: "",
      changedNodes: [],
      changedTitle: "",
      diffHead1: "Branch",
      diffHead2: "Trunk",
      removedNodes: [],
      removedTitle: "",
      same: false,
      title: "",
      supportsPassive: false,
    };
  },
  methods: {
    FlowDiff() {
      this.loading = true;
      this.retrieveResult = "";
      this.show_diff = false;
      this.axios
        .post("/api/changednodes", {
          brName: this.args.brname,
          revNo1: this.args.brev,
          revNo2: this.args.trev,
        })
        .then((response) => {
          this.retrieveResult = "";
          var f = response.data;
          console.log(f);
          if (String(f).startsWith("svn")) {
            this.retrieveResult = f;
            this.errmsg = "danger";
          } else {
            if (f.added.length === 0 && f.changed.length === 0 && f.removed.length === 0) {
              this.retrieveResult = "Flows are the same";
              this.errmsg = "success";
              this.show_diff = false;
              this.same = true;
            } else {
              this.addedNodes = f.added;
              this.addedTitle = "Nodes added (" + f.added.length + ")";
              this.changedNodes = f.changed;
              this.changedTitle = "Nodes changed (" + f.changed.length + ")";
              this.removedNodes = f.removed;
              this.removedTitle = "Nodes removed (" + f.removed.length + ")";
              this.show_diff = true;
              this.same = false;
              this.title = "Difference in Flows between " + this.args.brname + " rev " + this.args.brev + " and trunk rev " + this.args.trev; 
            }
          }
          this.loading = false;
        })
        .catch((error) => {
          if (error.response.status === 400) {
            this.errmsg = "success";
            this.retrieveResult = "Please push 'Retrieve' again.";
          } else if (error.response.status === 500) {
            this.errmsg = "danger";
            this.retrieveResult = "Please enter both revisions.";
          } else {
            this.errmsg = "danger";
            console.log(error.message);
          }
          this.loading = false;
        });
    },
    sync(event) {
      this.$nextTick(function() {
        this.reset(event);
      }
    )},
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
        if (updateX && Math.round(otherEl.scrollLeft - (scrollX = otherEl.eX = Math.round(xRate * (otherEl.scrollWidth - otherEl.clientWidth))))) { 
          otherEl.scrollLeft = scrollX; 
        }
        if (updateY && Math.round(otherEl.scrollTop - (scrollY = otherEl.eY = Math.round(yRate * (otherEl.scrollHeight - otherEl.clientHeight))))) {
          otherEl.scrollTop = scrollY;
        }
      }, this.supportsPassive ? { passive: true } : false);
    },
    reset(event) {
      var elems = this.$el.getElementsByClassName("syncscroll");
      var el1 = elems[0];
      var el2 = elems[1];
          // clearing existing listeners
      if (event.target.innerText === "Hide Code") {
        el1.removeEventListener('scroll', el1.syn, this.supportsPassive ? { passive: true } : false);
        el2.removeEventListener('scroll', el2.syn, this.supportsPassive ? { passive: true } : false);
            // setting-up the new listeners
              el1.eX = el1.eY = el2.eX = el2.eY = 0;
        this.syncScroll(el1, el2);
        this.syncScroll(el2, el1);
      }
    },
    backToOverview() {
      this.emitter.emit("BackToOverview");
    },
  },
  mounted() {
    // Test via a getter in the options object to see if the passive property is accessed
    this.supportsPassive = false;
    try {
      var opts = Object.defineProperty({}, 'passive', {
        get: function() {
          this.supportsPassive = true;
          return true;
        }
      });
      window.addEventListener("testPassive", null, opts);
      window.removeEventListener("testPassive", null, opts);
    } catch (e) { console.log(e); }
    this.emitter.on("FlowDifference", (args) => {
      console.log("received FlowDifference message");
      console.log(args);
      this.args = args;
      this.FlowDiff();
    });
    this.emitter.on("IsFlowDifferent", () => {
      console.log("received IsFlowDifferent message");
      this.emitter.emit("FlowIsDifferent", {result: !this.same});
    });
  },
};
</script>
<style></style>
