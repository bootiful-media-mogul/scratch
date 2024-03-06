<script lang="ts">

import {mogul, SettingsPage} from "@/services";
import {settings} from "@/services";

export default {


  methods: {},


  data() {
    const mogul = ''
    const settings: Array<SettingsPage> = []
    return {
      mogul, settings
    }
  },
  async created() {
    this.mogul = await mogul.me()
    const results = await settings.settings()
    console.log(JSON.stringify(results))
    this.settings = results
  }

}
</script>

<template>
  <h1 v-if="mogul">Settings for {{ mogul }} </h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>


      <div
          v-for="settingsPage in settings"
          v-bind:key="settingsPage.category"
      >



        <!--

         todo what other parts of the app could be  and should be translated using i18n plugin? like, everything, basically?

         todo get this to the point that it shows elegant
         label: <br/>
         <textarea ></textarea>

         with good form stylin and everything.
        -->

        <legend>
        <span>
        {{ $t('settings.' + settingsPage.category) }}
        </span>
        </legend>

        <!--        <h2>{{ $t( 'settings.' + settingsPage.category  )}}</h2>-->

        <div :key="setting.name" v-for="setting in settingsPage.settings">
          {{ $t('settings.' + settingsPage.category + '.' + setting.name) }} ::
          {{ setting.valid }}
        </div>


      </div>


      <div class="podcast-episode-controls-row">
        <span class="save">
           <button class="pure-button pure-button-primary" type="submit">
             save
           </button>
        </span>

      </div>
    </fieldset>
  </form>


</template>

<style>
</style>
