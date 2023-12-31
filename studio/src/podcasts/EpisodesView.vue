<script lang="ts">
import { Episode, Podcast, podcasts } from '@/services'
import AiWorkshopItIconComponent from '@/ai/AiWorkshopItIconComponent.vue'
import ManagedFileComponent from '@/managedfiles/ManagedFileComponent.vue'
import { reactive } from 'vue'
import { dateTimeFormatter } from '../dates'

export default {
  mounted(): void {
    this.loadPodcast()
  },

  components: {
    ManagedFileComponent,
    AiWorkshopItIconComponent
  },

  props: ['id'],

  methods: {
    dateTimeFormatter() {
      return dateTimeFormatter
    },

    async loadPodcast() {
      const newPodcastId = this.selectedPodcastId
      this.currentPodcast = await podcasts.podcastById(newPodcastId)
      this.episodes = await podcasts.podcastEpisodes(newPodcastId)
    },

    async deleteEpisode(episode: Episode) {
      await podcasts.deleteEpisode(episode.id)
      await this.cancel(new Event(''))
    },

    async loadEpisode(episode: Episode) {
      this.draftEpisode.id = episode.id
      this.draftEpisode.interview = episode.interview
      this.draftEpisode.introduction = episode.introduction
      this.draftEpisode.graphic = episode.graphic
      this.draftEpisode.title = episode.title
      this.draftEpisode.description = episode.description
      this.draftEpisode.complete = episode.complete
      this.draftEpisode.created = episode.created
      this.draftEpisode.availablePlugins = episode.availablePlugins
      this.description = this.draftEpisode.description
      this.title = this.draftEpisode.title
      this.created = this.draftEpisode.created
      this.dirtyKey = this.computeDirtyKey()

      const plugins = episode.availablePlugins
      if (plugins && plugins.length == 1) this.selectedPlugin = plugins[0]

      await this.loadPodcast()

      if (this.completionEventListenersEventSource === null && !this.draftEpisode.complete) {
        console.log(
          'going to install a listener for completion events for podcast episode [' +
            this.draftEpisode.id +
            ']'
        )
        const uri: string =
          '/api/podcasts/' +
          this.currentPodcast.id +
          '/episodes/' +
          this.draftEpisode.id +
          '/completions'
        console.log('the uri is ' + uri)
        this.completionEventListenersEventSource = new EventSource(uri)
        this.completionEventListenersEventSource.onmessage = (sseEvent: MessageEvent) => {
          console.log('got the following SSE event: ' + sseEvent.data)
          this.draftEpisode.complete = true
          this.completionEventListenersEventSource.close()
        }
        this.completionEventListenersEventSource.onerror = function (sseME: Event) {
          console.error('something went wrong in the SSE: ' + JSON.stringify(sseME))
        }
      }
    },

    async save(e: Event) {
      e.preventDefault()

      if (this.draftEpisode.id) {
        // we're editing a record, so update it
        const episode = await podcasts.updatePodcastEpisode(
          this.draftEpisode.id,
          this.title,
          this.description
        )
        await this.loadEpisode(episode)
      } //
      else {
        const episode = await podcasts.createPodcastEpisodeDraft(
          this.selectedPodcastId,
          this.title,
          this.description
        )
        await this.loadEpisode(episode)
      }
    },

    async publish(e: Event) {
      e.preventDefault()
      await podcasts.publishPodcastEpisode(this.draftEpisode.id, this.selectedPlugin)
    },
    pluginSelected(e: Event) {
      e.preventDefault()
      console.log('so you selected a plugin, didja ? it is ' + JSON.stringify(this.selectedPlugin))
    },

    /**
     * returns true if the buttons should be disabled because there's no change in the data in the form.
     */
    buttonsDisabled() {
      let changed = false
      if (!this.draftEpisode.id) {
        const hasData: boolean = this.description.trim() != '' && this.title.trim() != ''
        if (hasData) {
          changed = true
        }
      } else {
        changed = this.dirtyKey != this.computeDirtyKey()
      }

      return !changed
    },

    computeDirtyKey(): string {
      return (
        '' +
        (this.draftEpisode.id ? this.draftEpisode.id : '') +
        this.description +
        ':' +
        this.title
      )
    },

    async cancel(e: Event) {
      e.preventDefault()
      this.draftEpisode = reactive({} as Episode)
      this.title = ''
      this.description = ''
      await this.loadPodcast()
    }
  },

  created() {
    this.dirtyKey = this.computeDirtyKey()
    console.log('the dirty key is ' + this.dirtyKey)
  },

  setup() {
    console.log('setup called')
    return {}
  },

  data() {
    return {
      completionEventListenersEventSource: null as any as EventSource,
      completionEventListeners: [],
      selectedPlugin: '',
      created: -1,
      draftEpisode: reactive({} as Episode),
      episodes: [] as Array<Episode>,
      currentPodcast: null as any as Podcast,
      selectedPodcastId: this.id,
      title: '',
      description: '',
      dirtyKey: ''
    }
  }
}
</script>

<template>
  <h1 v-if="currentPodcast">Episodes for "{{ currentPodcast.title }}"</h1>

  <form class="pure-form pure-form-stacked">
    <fieldset>
      <legend>
        <span v-if="title">Editing "{{ title }}"</span>
        <span v-else> New Episode </span>
      </legend>

      <label for="episodeTitle">
        title
        <AiWorkshopItIconComponent
          prompt="please help me make the following podcast title more pithy and exciting"
          :text="title"
          @ai-workshop-completed="title = $event.text"
        />
      </label>
      <input id="episodeTitle" required v-model="title" type="text" />

      <label for="episodeDescription">
        description
        <AiWorkshopItIconComponent
          prompt="please help me make the following podcast description more pithy and exciting"
          :text="description"
          @ai-workshop-completed="description = $event.text"
        />
      </label>
      <textarea id="episodeDescription" rows="10" required v-model="description" />

      <div v-if="draftEpisode">
        <div v-if="draftEpisode.graphic" class="pure-g episode-managed-file-row">
          <div class="pure-u-3-24"><label>graphic</label></div>
          <div class="pure-u-21-24">
            <ManagedFileComponent
              accept=".jpg,.jpeg,.png,image/jpeg,image/jpg,image/png"
              v-model:managed-file-id="draftEpisode.graphic.id"
            />
          </div>
        </div>
        <div v-if="draftEpisode.introduction" class="pure-g episode-managed-file-row">
          <div class="pure-u-3-24"><label>introduction</label></div>
          <div class="pure-u-21-24">
            <ManagedFileComponent
              accept=".mp3,audio/mpeg"
              v-model:managed-file-id="draftEpisode.introduction.id"
            />
          </div>
        </div>
        <div v-if="draftEpisode.interview" class="pure-g episode-managed-file-row">
          <div class="pure-u-3-24"><label>interview</label></div>
          <div class="pure-u-21-24">
            <ManagedFileComponent
              v-model:managed-file-id="draftEpisode.interview.id"
              accept=".mp3,audio/mpeg"
            />
          </div>
        </div>
      </div>
      <div class="podcast-episode-controls-row">
        <span class="save">
          <button
            @click="save"
            :disabled="buttonsDisabled()"
            type="submit"
            class="pure-button pure-button-primary"
          >
            save
          </button>
        </span>
        <span class="cancel">
          <button
            @click="cancel"
            type="submit"
            :disabled="description == '' && title == ''"
            class="pure-button pure-button-primary"
          >
            cancel
          </button>
        </span>

        <div class="publish-menu">
          <select
            v-model="selectedPlugin"
            @change="pluginSelected"
            :disabled="!draftEpisode.complete"
          >
            <option disabled value="">Please select a plugin</option>

            <option
              v-for="(option, index) in draftEpisode.availablePlugins"
              :key="index"
              :value="option"
            >
              {{ option }}
            </option>
          </select>

          <button
            :disabled="!draftEpisode.complete"
            @click="publish"
            type="submit"
            class="pure-button pure-button-primary publish-button"
          >
            publish
          </button>
        </div>
      </div>
    </fieldset>
  </form>

  <form class="pure-form">
    <fieldset>
      <legend>Episodes</legend>

      <div class="pure-g form-row episodes-row" v-bind:key="episode.id" v-for="episode in episodes">
        <div class="id id-column">
          #<b>{{ episode.id }}</b>
        </div>
        <div class="created">{{ dateTimeFormatter().format(new Date(episode.created)) }}</div>
        <div class="edit"><a href="#" @click="loadEpisode(episode)" class="edit-icon"> </a></div>
        <div class="delete">
          <a href="#" @click="deleteEpisode(episode)" class="delete-icon"></a>
        </div>
        <div class="title">{{ episode.title }}</div>
      </div>
    </fieldset>
  </form>
</template>

<style>
.podcast-episode-controls-row {
  display: grid;
  grid-template-areas: 'save . cancel . publish';
  grid-template-columns: min-content var(--form-buttons-gutter-space) min-content auto min-content;
}

.podcast-episode-controls-row .save {
  grid-area: save;
}

.podcast-episode-controls-row .cancel {
  grid-area: cancel;
}

.podcast-episode-controls-row .publish-button {
  grid-area: publish-button;
}

.podcast-episode-controls-row .publish-menu {
  display: grid;
  grid-area: publish;
  grid-template-columns: min-content var(--form-buttons-gutter-space) min-content;
  grid-template-areas: '  publish-select  . publish-button';
}

.publish-menu button {
  grid-area: publish-button;
}

.publish-menu select {
  grid-area: publish-select;
}

.episodes-row {
  grid-template-areas: 'id created edit delete  title';
  grid-template-columns:
    var(--icon-column)
    10em
    var(--icon-column)
    var(--icon-column)
    auto;
  display: grid;
}

.episode-managed-file-row {
  height: calc(var(--gutter-space) * 1);
  margin-bottom: var(--gutter-space);
  margin-top: var(--gutter-space);
}

.episode-managed-file-row label {
  padding: 0;
  text-align: right;
  margin: 0 var(--gutter-space) 0 0;
}
</style>
