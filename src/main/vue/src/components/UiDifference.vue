<template>
  <b-card v-show="showUiDiff">
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
    <b-overlay :show="loading" variant="light" opacity="0.6" rounded="sm"></b-overlay>
  </b-card>
</template>

<script>
export default {
  name: "UiDifference",
  props: ["showUiDiff"],
  data() {
    return {
      loading: false,
      errmsg: "",
      retrieveResult: "",
      title: "Differences in UI files",
      same: false,
    };
  },
  methods: {
    uiDiff() {
      this.retrieveResult = "";
      this.loading = true;
      this.show_ui = true;
      this.show_diff = false;
      this.show_nr = true;
      this.axios.post("/api/changeduis")
      .then(response => {
        this.retrieveResult = "";
        var f = response.data;
        this.html = f.html;
        this.js = f.js;
        this.css = f.css;
        this.reset({target: {innerText: "HTML"}})
        this.loading = false;
      })
      .catch(error => {
        if (error.response.status === 400) {
          this.retrieveResult.dirty = "Please push button again.";
        } else {
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
    this.emitter.on('UiDifference', (args) => {
      console.log('received UiDifference message');
      console.log(args);
    });
    this.emitter.on("IsUiDifferent", () => {
      console.log("received IsUiDifferent message");
      this.emitter.emit("UiIsDifferent", {result: !this.same});
    });
  },
}
</script>
<style></style>
