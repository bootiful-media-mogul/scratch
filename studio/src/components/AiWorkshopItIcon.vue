<template>
  <a href="#" @click="workshop">workshop it!!</a>
</template>

<script lang="ts">
import { workshopInAi } from '@/services'

export default {
  props: ['text'],
  data() {
    return {
      description: ''
    }
  },
  methods: {
    workshop() {
      workshopInAi(this, this.text)
    }
  }
}
</script>
