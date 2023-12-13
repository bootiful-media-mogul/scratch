<template>
  <input class="managed-file-file-upload"
         ref="realFileUploadInputField"
         type="file"
         @change="uploadFile($event)"/>


  <div>

    <a href="#" @click="launchFileUpload">select a file </a>
    <span v-if="written">✅</span>
    <span v-if="uploading">🕒</span>
  </div>

</template>
<style>
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
import {ref} from "vue";

function asyncSetTimeout(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export default {


  async mounted() {
    await this.refreshManagedFile()
  },

  emits: ['update:managedFile'],
  props: ['disabled', 'managedFileId'],

  data() {
    return {
      managedFile: null as any as ManagedFile,
      written: ref(false),
      uploading: ref(false)
    }
  },

  methods: {

    launchFileUpload() {
      const realFileUploadInputField = this.$refs.realFileUploadInputField as HTMLElement
      realFileUploadInputField.click()

    },

    async refreshManagedFile() {
      console.log('the managed file id is ' + this.managedFileId)
      this.managedFile = await managedFiles.getManagedFileById(parseInt(this.managedFileId))
      this.written = this.managedFile.written
      console.log('written? ' + JSON.stringify(this.managedFile))
      console.log(this.managedFile ['written'])

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

      // const start = Date.now()
      //
      // const that = this

      //
      // while (!this.written && Date.now() < (start + 5000)) {
      //   await asyncSetTimeout(1000)
      //   await this.refreshManagedFile()
      // }
      await this.refreshManagedFile()

      this.$forceUpdate()
    }
  }
}
</script>
