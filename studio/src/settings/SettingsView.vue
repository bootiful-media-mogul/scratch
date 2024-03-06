<script lang="ts">

import {dateTimeFormatter} from '../dates'
import {mogul} from "../services";

export default {


  methods: {},

  data() {
    const mogul = ''
    return {
      mogul
    }
  },
  async created() {
    this.mogul = await mogul.me()
  }

}
</script>

<template>
  <h1 v-if="mogul">Settings for  {{ mogul }} </h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>
        <span> Global Settings  </span>
      </legend>

      <label for="episodeTitle">
        title
      </label>
      <input id="episodeTitle" required type="text"/>


      <div class="podcast-episode-controls-row">
        <span class="save">
           <button   class="pure-button pure-button-primary" type="submit">
             save
           </button>
        </span>

      </div>
    </fieldset>
  </form>


</template>

<style>
</style>
