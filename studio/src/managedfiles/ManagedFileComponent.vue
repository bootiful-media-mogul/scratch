<template>
  <input
    class="managed-file-file-upload"
    ref="realFileUploadInputField"
    type="file"
    :accept="accept ? accept : '*/*'"
    @change="uploadFile($event)"
  />

  <div class="managed-file-row">


    <span class="controls">
      <slot></slot>
    </span>

    <a class="choose" href="#" @click="launchFileUpload">
      <span class="folder-icon"></span>
    </a>

    <span class="written">
      <span v-if="uploading">🕒</span>
      <span v-else>
        <span :class="'mogul-icon checkbox-icon ' + (written ? '' : ' disabled')"></span>
      </span>
    </span>
    <span class="preview">
      <a
        href="#"
        :class="'mogul-icon preview-icon ' + (written ? '' : ' disabled')"
        @click="preview"
      >
      </a>
    </span>

    <span class="contentType">
      <span v-if="contentType">
        <code style="font-size: smaller">{{ contentType }}</code>
      </span>
    </span>

    <span class="filename">
      <span v-if="filename">{{ filename }} </span>
      <span class="form-prompt" v-else>{{ $t('managedfiles.please-upload-a-file') }}</span>
    </span>
  </div>
</template>
<style>
.managed-file-row {
  grid-template-areas: 'controls choose written  preview   contentType   filename';
  grid-template-columns:
    min-content
    var(--icon-column)
    var(--icon-column)
    var(--icon-column)
    15em
    auto;
  display: grid;
}
 .managed-file-row .controls {
   grid-area: controls  ;
 }
.managed-file-row .filename {
  grid-area: filename;
}

.managed-file-row .contentType {
  grid-area: contentType;
}

.managed-file-row .written {
  grid-area: written;
}

.managed-file-row .preview {
  grid-area: preview;
}

.managed-file-row .choose {
  grid-area: choose;
}

.managed-file-row a:hover {
  text-decoration: none;
}

/*
  hide the file upload off screen so it doesn't ruin the ui.
  (display: none doesn't work)
*/
.managed-file-file-upload {
  display: none;
  z-index: -1;
  opacity: 10;
  position: absolute;
  top: -1000px;
  left: -1000px;
}
</style>
<script lang="ts">
import axios from 'axios'
import { previewManagedFile, managedFiles } from '@/services'
import { ref } from 'vue'

export default {
  async mounted() {
    await this.loadManagedFileIntoEditor()
  },

  emits: ['update:managedFile'],
  props: ['disabled', 'accept', 'managedFileId'],

  watch: {
    async managedFileId(newVal: number, oldVal: number) {
      await this.loadManagedFileIntoEditor()
    }
  },
  data() {
    return {
      filename: ref(''),
      contentType: ref(''),
      size: ref(0),
      uploading: ref(false),
      written: ref(false)
    }
  },

  methods: {
    async preview() {
      if (this.written) {
        previewManagedFile(this.managedFileId)
      }
    },

    launchFileUpload() {
      const realFileUploadInputField = this.$refs.realFileUploadInputField as HTMLElement
      realFileUploadInputField.click()
    },

    async loadManagedFileIntoEditor() {
      const managedFile = await managedFiles.getManagedFileById(parseInt(this.managedFileId))
      this.filename = managedFile.filename
      this.written = managedFile.written
      this.contentType = managedFile.contentType
      this.size = managedFile.size
    },

    async uploadFile(event: any) {
      event.preventDefault()

      const data = new FormData()
      const file = event.target.files[0] as File
      data.set('file', file)
      const uploadPath: string = '/api/managedfiles/' + this.managedFileId
      this.uploading = true
      const response = await axios.post(uploadPath, data, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      console.assert(
        response.status >= 200 && response.status <= 300,
        'the http post to upload the archive did not succeed.'
      )

      this.written = true
      this.uploading = false
      await this.loadManagedFileIntoEditor()
    }
  }
}
</script>
