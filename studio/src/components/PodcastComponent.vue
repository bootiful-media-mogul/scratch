<template>

  <div>


    <h3>
      <span>{{podcast.id}}</span>
      {{podcast.title}}
    </h3>

  </div>


</template>

<script lang="ts">



import {Podcast} from "@/model"

export default {
  props: {
    podcast: Podcast
  },
  setup(props) {
    console.log('props: ' + props)

  },
}
</script>
