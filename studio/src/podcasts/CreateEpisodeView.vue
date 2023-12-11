<script lang="ts">
import { defineComponent, ref } from 'vue'
import ManagedFileComponent from '@/managedfiles/ManagedFileComponent.vue'

export default defineComponent({
  components: { ManagedFileComponent },
  setup(props, ctx) {
    return {
      intro: ref(null as any),
      interview: ref(null as any),
      photo: ref(null as any)
    }
  }
})
</script>
<template>
  <div>
    <b>photo</b>
    <ManagedFileComponent v-model:managed-file-id="photo" />
  </div>
  <div>
    <b>introduction</b>
    <ManagedFileComponent v-model:managed-file-id="intro" />
  </div>
  <div>
    <b>interview</b>
    <ManagedFileComponent v-model:managed-file-id="interview" />
  </div>
</template>
