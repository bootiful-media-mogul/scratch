<template>
  <input
      class="managed-file-file-upload"
      ref="realFileUploadInputField"
      type="file"
      @change="uploadFile($event)"
  />

  <div class="managed-file-row pure-g">



    <a class="choose pure-u-1-24" href="#" @click="launchFileUpload">
      <img alt="select a file" width="20" src="../assets/images/folder.png"/>
    </a>

     <span class="written pure-u-1-24">
      <span v-if="uploading">🕒</span>
      <span v-else>
       <span v-if="written">
         <img alt="select a file" width="20" src="../assets/images/checkbox.png"/>
       </span>
      </span>
    </span>


    <span class="filename pure-u-21-24">
    <span v-if="filename">  {{ filename }} </span>
    <span class="form-prompt" v-else>
     (please upload a file)
    </span>

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
import {managedFiles} from '@/services'
import {ref} from 'vue'

export default {
  async mounted() {
    await this.loadManagedFileIntoEditor()
  },

  emits: ['update:managedFile'],
  props: ['disabled', 'managedFileId'],

  watch: {
    async managedFileId(newVal: number, oldVal: number) {
      await this.loadManagedFileIntoEditor()
    }
  },
  data() {
    return {
      filename: ref(''),
      size: ref(0),
      uploading: ref(false),
      written: ref(false),
    }
  },


  methods: {
    launchFileUpload() {
      const realFileUploadInputField = this.$refs.realFileUploadInputField as HTMLElement
      realFileUploadInputField.click()
    },

    async loadManagedFileIntoEditor() {
      const managedFile = await managedFiles.getManagedFileById(parseInt(this.managedFileId))
      this.filename = managedFile.filename
      this.written = managedFile.written
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
