<template>
  <div v-if="showNotification">
    <div class="body-overlay"></div>
    <div class="notification-panel">

      {{ latestNotification }}
      <div class="buttons">
        <button @click.prevent="ok()" type="submit">
          {{ $t('ok') }}
        </button>
      </div>
    </div>
  </div>
</template>
<style scoped>


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

.notification-panel {
  /* */
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  /* */
  z-index: 20001;

  border-radius: var(--gutter-space);
  color: white;
  padding: var(--gutter-space);
  background-color: black;
}

.notification-panel .buttons a {
  color: white;
}

/*

#notification {
  background-color: white;
  padding: var(--gutter-space);
  text-align: center;
  border-radius: 10px 10px 10px 10px;
}

.notification-text {

}

.notification-visible {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 20px;
  background-color: white;
  border: 1px solid #ccc;

  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  opacity: 1;
  transition: opacity 0.5s ease;
}

.notification-hidden {
  opacity: 0;
}
*/

</style>

<script lang="ts">
import { notifications, Notification } from '@/services'
import { ref } from 'vue'

export default {
  components: {},
  computed: {},
  methods: {

    hide() {

    },
    ok() {

      this.showNotification = false
      console.log(`let me guess: you want me to stop showing this dialog?`)
    }
  },

  data() {
    return {
      showNotification: false,
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

      that.showNotification = true

    }

    const processorRef: (notification: Notification) => void = processor
    notifications.listen(processorRef)

  }
}
</script>
