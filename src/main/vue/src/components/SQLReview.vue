<template>
  <b-card title="Overview" style="margin: auto; width: fit-content" v-show="showPage">
    <b-alert v-show="retrieveResult !== ''" :variant="errmsg" show>{{ retrieveResult }}</b-alert>
    <b-overlay :show="loading" variant="light" opacity="0.6" rounded="sm"></b-overlay>
    <label>{{selBranch}}</label>
    <b-form @submit="onSubmitBranch">
      <b-container fluid style="padding-left: 0">
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
  </b-card>
</template>

<script>
export default {
  name: "CreateBranch",
  props: ["showPage"],
  data() {
    return {
      curBranchInfo: {},
      curBranchName: "",
      wcLocation: "",
      currentBranchRev: 0,
      remoteUrl: "",
      branches: [],
      selBranch: '',
      latestBranchRevs: [],
      latestTrunkRevs: [],
      highestTrunkRevInBranch: 0,
      loading: false,
      errmsg: "",
      retrieveResult: "",
    };
  },
  methods: {
  },
  mounted() {
    this.emitter.on('GetBranches', () => {
      console.log('GetBranches message')
      this.getBranches();
    });
  },
}
</script>
<style></style>
