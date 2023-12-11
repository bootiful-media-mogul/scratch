import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './HomeView.vue'
import CreateEpisodeView from '@/podcasts/CreateEpisodeView.vue'
import CreatePodcastView from '@/podcasts/PodcastsView.vue'
// import ListPodcastsView from './ListPodcastsView.vue'
// import CreatePodcastView from '@/CreateEpisodeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/create-podcast',
      name: 'create-podcast',
      component: CreatePodcastView
    }
    /* {
      path: '/list-podcasts',
      name: 'list-podcasts',
      component: ListPodcastsView
    },
    {
      path: '/create-podcast',
      name: 'create-podcast',
      component: CreatePodcastView
    }*/
  ]
})

export default router
