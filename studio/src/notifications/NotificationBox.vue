<template>
  <div v-if="showModalNotification">
    <div class="body-overlay"></div>
    <div class="modal-notification-panel">

      {{ latestNotification }}
      <div class="buttons">
        <button @click.prevent="dismiss()" type="submit">
          {{ $t('ok') }}
        </button>
      </div>
    </div>
  </div>

  <div v-if="showToasterNotification">

 <div class="toaster-notification-panel">
   {{latestNotification}}
 </div>

  </div>
</template>
<style scoped>

.toaster-notification-panel {
 position: fixed;
  bottom: 0;
}

.body-overlay {
  height: 100%;
  background-color: white;
  width: 100%;
  z-index: 1000;
  position: fixed;
  opacity: 0.7;
  top: 0;
  left: 0
}

.modal-notification-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 20001;
  border-radius: var(--gutter-space);
  color: white;
  padding: var(--gutter-space);
  background-color: black;
}

.modal-notification-panel .buttons a {
  color: white;
}


</style>

<script lang="ts">
import { notifications, Notification } from '@/services'
import { ref } from 'vue'

export default {
  components: {},
  computed: {},
  methods: {

    dismiss() {
      this.showToasterNotification = false
      this.showModalNotification = false
    }
  },

  data() {
    return {
      showToasterNotification: false,
      showModalNotification: false,
      notification: ref(null),
      latestNotification: '' as string
    }
  },
  async created() {

    const that = this


    function processor(notification: Notification) {

      that.latestNotification = that.$t('notifications.' + notification.category, {
        key: notification.key,
        mogulId: notification.mogulId,
        when: notification.when,
        context: notification.context
      })


      that.showModalNotification = notification.modal
      that.showToasterNotification = !notification.modal

    }

    const processorRef: (notification: Notification) => void = processor
    notifications.listen(processorRef)

  }
}
</script>
