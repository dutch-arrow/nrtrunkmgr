<template>
<b-container fluid>
  <b-row>
    <b-col>
      <b-card title="Enter your SVN credentials" style="margin:auto;width:fit-content">
        <b-form @submit="handleLoginSubmit">
          <b-container>
              <b-row>
                <b-col cols="3">
                  <label class="col-form-label col-lg-1" for="name-input" style="width:auto;">Username</label>
                </b-col>
                <b-col cols="9">
                  <b-form-input id="name-input" v-model="uname" aria-autocomplete="name" :state="nameState" required aria-describedby="input-required1"></b-form-input>
                  <b-form-invalid-feedback id="input-required1">Input is required</b-form-invalid-feedback>
                </b-col>
              </b-row>
              <b-row>
                <b-col cols="3">
                  <label class="col-form-label col-lg-1" for="pwd-input" style="width:auto;">Password</label>
                </b-col>
                <b-col cols="9">
                  <b-form-input id="pwd-input" v-model="pwd" type="password" aria-autocomplete="current-password" :state="pwdState" required aria-describedby="input-required2"></b-form-input>
                  <b-form-invalid-feedback id="input-required2">Input is required</b-form-invalid-feedback>
                </b-col>
              </b-row>
              <b-row class="mt-2">
                <b-button type="submit" variant="primary">Save</b-button>
              </b-row>
          </b-container>
        </b-form>
      </b-card>
    </b-col>
  </b-row>
</b-container>
</template>

<script>
export default {
  name: 'BranchMgr',
  emits: ['hideLogin'],
  data() {
    return {
      showLogin: false,
      // Login dialog data
      uname: "",
      nameState: null,
      pwd: "",
      pwdState: null,
    }
  },
  methods: {
    /*
    * Store a cookie with the give name, value and expiration days
    */
    setCookie(cname, cvalue, exdays) {
      const d = new Date();
      d.setTime(d.getTime() + (exdays*24*60*60*1000));
      let expires = "expires="+ d.toUTCString();
      document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
    },
    /*
    * Store the cookie values from the Login dialog
    */
    handleLoginSubmit() {
      var valid = true;
      if (this.uname === "") {
        this.nameState = false;
        valid = false;
      }
      if (this.pwd === "") {
        this.pwdState = false;
        valid = false;
      }
      if (!valid)  return;
      this.nameState = true;
      this.pwdState = true;
      this.showLogin = false;
      this.$emit('hideLogin');
      this.setCookie("SvnUser", this.uname, 365);
      this.setCookie("SvnPwd", this.pwd, 365);
    }
  },
  // Lifecycle hooks are called at different stages of a component's lifecycle.
  // This function will be called when the component is mounted.
  mounted() {
    
  }
}
</script>

<style scoped>

</style>
