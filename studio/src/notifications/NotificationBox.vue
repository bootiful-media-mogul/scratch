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

  <div v-if="showToasterNotification" :class="toasterNotificationCss">
    {{ latestNotification }}
  </div>
</template>
<style>
.toaster-notification-panel {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.5); /* horizontal-offset vertical-offset blur-radius color */
  border-radius: 0 0 var(--gutter-space) var(--gutter-space);
  background-color: white;
  padding: var(--gutter-space);
  position: fixed;
  width: 40%;
  transition: transform 2s ease;
  top: 0;
  left: 30%;
  transform: translate(-50%);
  text-align: center;
}

.animated-element-visible {
  transform: translateY(0);
}

.animated-element-hidden {
  transform: translateY(-100px);
}

.body-overlay {
  height: 100%;
  background-color: white;
  width: 100%;
  z-index: 1000;
  position: fixed;
  opacity: 0.7;
  top: 0;
  left: 0;
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
    },
    show() {
      this.toasterNotificationCss = {
        'toaster-notification-panel': true,
        'animated-element-visible': true,
        'animated-element-hidden': false
      }
    },
    hide() {
      this.toasterNotificationCss = {
        'toaster-notification-panel': true,
        'animated-element-visible': false,
        'animated-element-hidden': true
      }
    }
  },

  data() {
    return {
      nextTimeoutId: 0,
      toasterNotificationCss: {},
      showToasterNotification: true,
      showModalNotification: false,
      notification: ref(null),
      latestNotification: '' as string
    }
  },
  async created() {
    const that = this
    this.toasterNotificationCss = {
      'toaster-notification-panel': true,
      'animated-element-hidden': true
    }
    function processor(notification: Notification) {
      that.latestNotification = that.$t('notifications.' + notification.category, {
        key: notification.key,
        mogulId: notification.mogulId,
        when: notification.when,
        context: notification.context
      })

      that.showModalNotification = notification.modal
      that.showToasterNotification = !notification.modal

      const displayForNMilliseconds = 1000 * 5 // 2 seconds
      if (that.showToasterNotification) {
        that.show()
        clearTimeout(that.nextTimeoutId)
        that.nextTimeoutId = setTimeout(function (e: Event) {
          that.hide()
        }, displayForNMilliseconds)
      }
    }
    const processorRef: (notification: Notification) => void = processor
    notifications.listen(processorRef)
  }
}
</script>
