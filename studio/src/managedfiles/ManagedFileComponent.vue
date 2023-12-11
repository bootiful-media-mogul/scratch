<template>
  <div>
    <form ref="fileUploadForm">
      <input type="file" @change="uploadFile($event)"/>
    </form>
  </div>
</template>

<script lang="ts">
import axios from 'axios'
import {ManagedFile, managedFiles} from '@/services'

export default {
  emits: ['update:managedFile'],
  props: {
    managedFile: {
      type: ManagedFile
    }
  },
  methods: {
    async uploadFile(event: any) {
      event.preventDefault()

      const data: FormData = new FormData(this.$refs.fileUploadForm as HTMLFormElement)
      const file: File = event.target.files[0] as File
      data.set('file', file)

      const mf = await managedFiles.getManagedFileById(12)
      const uploadPath: string = '/api/managedfiles/' + mf.id
      const response = await axios.post(uploadPath, data, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      console.assert(
          response.status >= 200 && response.status <= 300,
          'the http post to upload the archive did not succeed.'
      )
      console.log('uploaded to ' + uploadPath + ' vis ' + uploadPath)
      this.$emit('update:managedFile', await managedFiles.getManagedFileById(mf.id))
    }
  }
}
</script>
