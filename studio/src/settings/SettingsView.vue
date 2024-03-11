<script lang="ts">
import { mogul, Setting, Settings, settings, SettingsPage } from '@/services'

export default {
  methods: {
    async reloadSettings() {
      this.settings = await settings.settings()

      // clone the settings so we can do dirty checking
      const newSettings: Array<SettingsPage> = []
      this.settings.forEach((sp: SettingsPage) => {
        const sub = sp.settings.map(
          (setting) => new Setting(setting.name, setting.valid, setting.value)
        )
        const nsp = new SettingsPage(sp.valid, sp.category, sub)
        newSettings.push(nsp)
      })
      this.loadedSettings = newSettings
    },
    async save(category: string) {
      const pageForCategory = this.settings.filter((sp) => sp.category == category)[0]
      const loadedPageForCategory = this.loadedSettings.filter((sp) => sp.category == category)[0]

      function findSettingWithMatchingKey(settings: Array<Setting>, k: string): Setting {
        const matches = settings.filter((s) => s.name == k)
        if (matches && matches.length > 0) return matches[0]
        return null as any
      }

      const loadedSettings = loadedPageForCategory.settings
      const updatedSettings = pageForCategory.settings
      for (const setting of updatedSettings) {
        const matching = findSettingWithMatchingKey(loadedSettings, setting.name)
        if (matching != null && matching.value != setting.value) {
          await settings.updateSetting(pageForCategory.category, setting.name, setting.value)
          await this.reloadSettings()
        }
      }
    }
  },

  data() {
    const loadedSettings: Array<SettingsPage> = []
    const mogul = ''
    const settings: Array<SettingsPage> = []
    return {
      mogul,
      settings,
      loadedSettings
    }
  },

  async created() {
    this.mogul = await mogul.me()
    await this.reloadSettings()
  }
}
</script>

<template>
  <h1 v-if="mogul">{{ $t('settings.title', { mogul: mogul }) }}</h1>
  <div v-for="settingsPage in settings" v-bind:key="settingsPage.category">
    <form class="pure-form pure-form-stacked">
      <fieldset>
        <legend>
          {{ $t(settingsPage.category) }}
        </legend>

        <div v-for="setting in settingsPage.settings" v-bind:key="setting.name">
          <div class="pure-control-group">
            <label :for="'aligned-name-' + setting.name">
              {{ $t('settings.' + settingsPage.category + '.' + setting.name) }}
            </label>

            <textarea
              :required="!setting.valid"
              :id="'aligned-name-' + setting.name"
              v-model="setting.value"
            >
            </textarea>

            <span class="pure-form-message-inline">
              <span v-if="!setting.valid"> {{ $t('labels.required-value') }}</span>
            </span>
          </div>
        </div>

        <div class="pure-controls">
          <button
            @click.prevent="save(settingsPage.category, settingsPage.settings)"
            type="submit"
            class="pure-button pure-button-primary"
          >
            {{ $t('settings.save-button', { plugin: $t(settingsPage.category) }) }}
          </button>
        </div>
      </fieldset>
    </form>
  </div>
</template>

<style scoped>
.pure-controls .pure-button {
  margin-top: calc(0.5 * var(--gutter-space));
}
</style>
