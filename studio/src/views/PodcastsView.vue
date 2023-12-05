<template>


  <div>
    <h1> Podcasts </h1>
  </div>
  <div v-bind:key="podcast.id" v-for="podcast in podcasts">
    <PodcastComponent :podcast="podcast"/>
  </div>
</template>

<script lang="ts">


import MogulClient from "@/mogulClient";
import PodcastComponent from "@/components/PodcastComponent.vue";

const api = new MogulClient();


export default {
  components: {PodcastComponent},
  data() {
    return {
      // todo replace with a dynamic call to the backend
      podcasts: []
    }
  },
  async created() {
    console.log('this is where i should be loading all the podcasts.')

    this.podcasts = await api.podcasts()
    console.log('podcasts: '+ this.podcasts)
  },
}

</script>