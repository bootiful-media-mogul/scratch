<template>
  <input type="file" @change="uploadFile($event)"/>
  <span v-if="written">✅</span>
  <span v-if="uploading">
    🕒 ss
  </span>
</template>

<script lang="ts">
import axios from 'axios'
import {ManagedFile, managedFiles} from '@/services'

export default {


  async mounted() {
    await this.refreshManagedFile()
  },

  emits: ['update:managedFile'],
  props: [
    'disabled',
    'managedFileId'
  ],
  data(vm) {
    return {
      managedFile: (null as any) as ManagedFile,
      written: false,
      uploading: false
    }
  },
  methods: {


    async refreshManagedFile() {
      this.managedFile = await managedFiles.getManagedFileById(parseInt(this.managedFileId))
      this.written = this.managedFile.written
      console.log('written? ' + this.written)
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

      await this.refreshManagedFile()
      this.uploading = false
      this.$emit('update:managedFile', this.managedFile)
      this.$forceUpdate()
    }
  }
}
</script>
