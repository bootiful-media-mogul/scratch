<template>


  <div>
    <h1> Podcasts </h1>
  </div>
  <div v-bind:key ="podcast.id" v-for="podcast in  podcasts">
    <PodcastComponent :podcast="podcast"/>
  </div>
</template>

<script lang="ts">


import {Podcast} from "@/model";
import PodcastComponent from "@/components/PodcastComponent.vue";

export default {
  components: {PodcastComponent},
  data() {
    return {
      // todo replace with a dynamic call to the backend
      podcasts: [
        new Podcast(1, 'A title'),
        new Podcast(2, 'Another title'),
      ]
    }
  },
  async created() {
    console.log('this is where i should be loading all the podcasts.')
  },
}

</script>