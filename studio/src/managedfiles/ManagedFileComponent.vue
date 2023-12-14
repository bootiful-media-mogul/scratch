<template>
  <input
      class="managed-file-file-upload"
      ref="realFileUploadInputField"
      type="file"
      @change="uploadFile($event)"
  />

  <div class="managed-file-row pure-g">

    <span class="written pure-u-1-24">
      <span v-if="uploading">🕒</span>
      <span v-else>
       <span v-if="managedFile.written">  <img alt="select a file" width="20" src="../assets/images/checkbox.png"/>
</span>
      </span>
    </span>

    <a class="choose pure-u-1-24" href="#" @click="launchFileUpload">
  <img alt="select a file" width="20" src="../assets/images/folder.png"/>
    </a>

    <span class="filename pure-u-21-24">
      {{ managedFile.filename }}
    </span>

  </div>
</template>
<style>

.managed-file-row {
  height: calc(var(--gutter-space) * 1);

}

.managed-file-row a:hover {
  text-decoration: none;
}

.managed-file-row .choose {
}


.managed-file-file-upload {
  border: 1px solid red;
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
import {ManagedFile, managedFiles} from '@/services'
import {reactive, ref} from 'vue'

export default {
  async mounted() {
    await this.refreshManagedFile()
  },

  emits: ['update:managedFile'],
  props: ['disabled', 'managedFileId'],

  watch: {
    async managedFileId(newVal: number, oldVal: number) {
      await this.refreshManagedFile()
    }
  },
  data() {
    return {
      managedFile: reactive({} as ManagedFile),
      uploading: ref(false)
    }
  },

  methods: {
    launchFileUpload() {
      const realFileUploadInputField = this.$refs.realFileUploadInputField as HTMLElement
      realFileUploadInputField.click()
    },

    async refreshManagedFile() {
      console.log( 'getting the managed file for id ' + this.managedFileId)
      const nMF = await managedFiles.getManagedFileById(parseInt(this.managedFileId))
      console.log( JSON.stringify(nMF))
      this.managedFile.id = nMF.id
      this.managedFile.size = nMF.size
      this.managedFile.written = nMF.written
      this.managedFile.folder = nMF.folder
      this.managedFile.filename = nMF.filename
      console.log('written? ' + JSON.stringify(this.managedFile))
    },

    async uploadFile(event: any) {
      event.preventDefault()
      console.log('the managed file : ' + this.managedFileId)
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
      this.uploading = false
      this.$emit('update:managedFile', this.managedFile)
      await this.refreshManagedFile()
      this.$forceUpdate()
    }
  }
}
</script>
