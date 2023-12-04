<script setup lang="ts">
defineProps<{
  msg: string
}>()
</script>

<template>
   <div>
     <h3>
       {{ msg }}
     </h3>
   </div>
</template>

<style scoped>
</style>
