<template>
  <b-navbar type="dark" variant="primary">
    <b-dropdown id="dropdown-1" text="Menu" variant="primary">
      <b-dropdown-item-button v-on:click="showThePage('home')">Home</b-dropdown-item-button>
    </b-dropdown>
    <div class="navbar-menu-title">Web UI</div>
    <b-button v-on:click="resetCredentials" class="mr-2" variant="primary" style="float:right">Reset credentials</b-button>
   </b-navbar>
    <login v-show="showLogin" @hideLogin="showLogin"></login>
    <home :showPage="activePage === 'home' && !showLogin"></home>
</template>

<script>
import Login from './components/Login.vue'
import Home from './components/Home.vue'

export default {
  name: 'TrunkMgr',
  components: {
    Home,
    Login,
  },
  data() {
    return {
      errorMsgs:"",
      showLogin: false,
      // Login dialog data
      uname: "",
      nameState: null,
      pwd: "",
      pwdState: null,
      activePage: "",
      curBranchInfo: {},
    }
  },
  methods: {
    /*
    * Retrieve a cookie with the give name
    */
    getCookie(cname) {
      let name = cname + "=";
      let decodedCookie = decodeURIComponent(document.cookie);
      let ca = decodedCookie.split(';');
      for(let i = 0; i <ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') { c = c.substring(1); }
        if (c.indexOf(name) == 0) {  return c.substring(name.length, c.length); }
      }
      return "";
    },
    /*
    * Check if there are cookies stored. If not, show dialog
    */
    checkCookies() {
      this.uname = this.getCookie("SvnUser");
      this.pwd = this.getCookie("SvnPwd");
      if (this.uname === "" || this.pwd === "") {
        this.showLogin = true;
      }
    },
    showThePage(page) {
      this.activePage = page;
      if (page === 'home') this.emitter.emit('GetBranches');
    },
    /*
    * Handle the "Reset credentials"-button press
    */
    resetCredentials() {
      console.log("Reset credentials");
      this.showLogin = true;
    },
  },
  // Lifecycle hooks are called at different stages of a component's lifecycle.
  // This function will be called when the component is mounted.
  mounted() {
    this.emitter.on('SetCurBranch', (branchInfo) => {
      this.curBranchInfo = branchInfo;
    });
    this.checkCookies();
    this.showThePage("home");
  }
}
</script>

<style>
.navbar-menu-title {
  margin: auto;
  color: white !important;
  font-size: 24px;
}
label {
  color:darkblue;
  font-weight: bolder;
}
.card-title {
  color:darkblue;
  text-align: center;
}
.code3 {
	font-family: "Monaco", "Courier New", monospace;
	font-size:smaller;
	height: 750px;
	width: 100%;
	white-space: nowrap;
	overflow:scroll;
}
.code2 {
	font-family: "Monaco", "Courier New", monospace;
	font-size:smaller;
	height: 600px;
	width: 100%;
	white-space: nowrap;
	overflow:scroll;
}
.code1 {
	font-family: "Monaco", "Courier New", monospace;
	font-size:smaller;
	height: 600px;
	width: 100%;
	overflow:scroll;
}
.col-code { width: 45%;	}
.del { background-color:orange; }
.add { background-color:lightgreen; }
.nav-link {
  font-size: 20px;
  color: darkblue;
}
.closeButton {
  color:red;
  align-self: flex-end;
}
</style>