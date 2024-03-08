<template>
  this is a notification box!: <br/>
  {{ latestNotification}}
</template>
<style>
</style>

<script lang="ts">
import {
  notifications, Notification
} from '@/services'

export default {
  components: {},
  computed: {},
  methods: {},
  data() {
    return {
      latestNotification: '' as string
    }
  },
  async created() {

    const that = this

    function processor(notification: Notification) {
      const str = JSON.stringify(notification)
      that.latestNotification = str
      console.log( 'showing: '  ,str)
    }

    const processorRef: (notification: Notification) => void = processor
    notifications.listen(processorRef)

  }
}
</script>
