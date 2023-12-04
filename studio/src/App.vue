<script setup lang="ts">
  const user =await window.fetch('/api/me')
  console.log( 'the user is ' +user)
</script>

<template>
 <h1> Hello </h1>
</template>
