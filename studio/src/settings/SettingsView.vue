<script lang="ts">
import { mogul, settings, SettingsPage } from '@/services'

export default {
  methods: {
    save: function  ( category: string ){
      const pageForCategory = this.settings.filter( (sp:SettingsPage) => sp.category == category)[0]

      console.log('Going to save updated configuration values for ' + category + '. ' +
        'The page for the category is '  , pageForCategory
      )

    }
  },


  data() {
    const mogul = ''
    const settings: Array<SettingsPage> = []
    return {
      mogul,
      settings
    }
  },
  async created() {
    this.mogul = await mogul.me()
    this.settings = await settings.settings()
  }
}
</script>

<template>


  <h1 v-if="mogul">{{ $t('settings.title',{mogul:mogul})}}</h1>
  <div
    v-for="settingsPage in settings"
    v-bind:key="settingsPage.category"
  >
    <form class="pure-form pure-form-stacked">

      <fieldset>

        <legend>
          {{  $t(settingsPage.category )}}
        </legend>

        <div
          v-for="setting in settingsPage.settings"
          v-bind:key="setting.name"

        >
          <div class="pure-control-group">
            <label :for="'aligned-name-' + setting.name">
              {{ $t('settings.' + settingsPage.category + '.' + setting.name) }}
            </label>

            <textarea :required ="!setting.valid" :id="'aligned-name-' + setting.name" v-model="setting.value">
            </textarea>

            <span class="pure-form-message-inline">
               <span v-if="!setting.valid"> {{ $t('labels.required-value') }}</span>
            </span>
          </div>
        </div>

        <div class="pure-controls">
          <button @click.prevent="save( settingsPage.category , settingsPage.settings )" type="submit" class="pure-button pure-button-primary">
              {{ $t('settings.save-button', { plugin: $t(settingsPage.category) })}}
          </button>
        </div>

      </fieldset>


    </form>
  </div>
</template>

<style scoped>

.pure-controls .pure-button {
   margin-top: calc(0.5 * var(--gutter-space))
}


</style>
