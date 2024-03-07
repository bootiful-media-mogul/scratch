import './assets/main.css'

import { createApp } from 'vue'
import { createI18n } from 'vue-i18n'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './index'

// i18n
const translations = {
  en: {

    labels : {
      'required-value' : 'this is a required value!'
    } ,

    'podbean' : 'Podbean',
    'github' : 'GitHub',

    'settings.title' : 'System Settings for {mogul}',
    'settings.save-button' : 'save configuration changes for {plugin}' ,


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
