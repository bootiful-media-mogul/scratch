<template>
  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>Media Preview</legend>

      <div>
        <img
          v-if="isImage"
          class="managed-file-preview-image"
          :alt="'a preview for managed file ' + managedFileId"
          :src="url"
        />

        <audio :src="url" v-if="isAudio" class="managed-file-preview-audio" controls></audio>
      </div>
      <div>
        <div>
          <b>{{ filename }}</b>
        </div>
        <div>
          <span style="font-size: smaller">
            <code>{{ contentType }}</code>
          </span>
        </div>
        <div>
          <span style="font-size: smaller">
            <code>{{ size }}</code>
          </span>
        </div>
      </div>
    </fieldset>
  </form>
</template>
<style></style>
<script lang="ts">
import { events, managedFiles } from '@/services'
import { prettyPrintInBytes } from '@/managedfiles/files'
import { ref } from 'vue'

export default {
  async created() {
    const callback = async (id: any) => await this.doLoad(id)

    events.on('preview-managed-file-event', async (event) => {
      console.log('received an event for a preview for managed file ' + event)
      await callback(event)
      events.emit('sidebar-panel-opened', this.$el)
    })
  },
  async mounted() {
    await this.load()
  },

  props: ['managedFileId'],

  watch: {
    async managedFileId(newVal: number, oldVal: number) {
      await this.load()
    }
  },
  data() {
    return {
      isImage: ref(false),
      isAudio: ref(false),
      url: ref(''),
      filename: ref(''),
      size: ref(''),
      contentType: ref('')
    }
  },
  methods: {
    async doLoad(mfid: any) {
      const managedFile = await managedFiles.getManagedFileById(parseInt(mfid))
      this.url = '/api/managedfiles/' + managedFile.id
      const ext = managedFile.contentType.toLowerCase()
      this.isImage =
        ext.endsWith('jpg') || ext.endsWith('jpeg') || ext.endsWith('png') || ext.endsWith('gif')
      this.isAudio = ext.endsWith('mp3') || ext.endsWith('wav') || ext.endsWith('mpeg')
      this.contentType = ext
      this.size = prettyPrintInBytes(managedFile.size)
      this.filename = managedFile.filename
    },

    async load() {
      if (!this.managedFileId) return

      await this.doLoad(this.managedFileId)
    }
  }
}
</script>
