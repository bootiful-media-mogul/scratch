import './assets/main.css'

import { createApp } from 'vue'
import { createI18n } from 'vue-i18n'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './index'

// i18n
const translations = {
  en: {

    labels: {
      'required-value': 'this is a required value!'
    },

    'podbean': 'Podbean',
    'github': 'GitHub',

    'episodes.title': 'all episodes',
    'episodes.episodes': 'episodes for {title}',
    'episodes.new-episode': 'new episode',
    'episodes.buttons.publish': 'publish',
    'episodes.buttons.cancel': 'cancel',
    'episodes.editing-episode':'editing "{title}"' ,
    'episodes.buttons.save': 'save',
    'episodes.episode.description.ai-prompt': 'please help me make this podcast episode description fun and pithy',
    'episodes.episode.title.ai-prompt': 'please help me make this podcast episode title fun and pithy',
    'episodes.episode.description': 'description',
    'episodes.episode.title': 'title',
    'episodes.plugins.please-select-a-plugin': 'please select a plugin',


    //
    'podcasts.title': 'Podcasts',
    'podcasts.new-podcast': 'New Podcast',
    'podcasts.new-podcast.title': 'title',
    'podcasts.title.ai.prompt': `please help me take the following podcast title and make it more pithy and exciting!`,
    'podcasts.new-podcast.submit': 'create a new podcast',
    'podcasts.podcasts.delete': 'delete',
    'podcasts.podcasts.episodes': 'episodes',

    'settings.title': 'System Settings for {mogul}',
    'settings.save-button': 'save configuration changes for {plugin}',


    'settings.github': 'Github',
    'settings.github.clientId': 'Client ID',
    'settings.github.clientSecret': 'Client Secret',

    'settings.podbean': 'Podbean',
    'settings.podbean.clientId': 'Client ID',
    'settings.podbean.clientSecret': 'Client Secret'
  }
}

const i18n = createI18n({
  locale: 'en',
  fallbackLocale: 'en',
  messages: translations
})

const app = createApp(App)

app.use(i18n)
app.use(router)
app.use(createPinia())

app.mount('#app')
