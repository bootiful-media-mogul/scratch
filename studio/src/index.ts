import { createRouter, createWebHistory } from 'vue-router'
import HomeView from './HomeView.vue'
import EpisodesView from '@/podcasts/EpisodesView.vue'
import PodcastsView from '@/podcasts/PodcastsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/podcasts',
      name: 'podcasts',
      component: PodcastsView
    },
    {
      path: '/podcast-episodes/:id',
      name: 'podcast-episodes',
      component: EpisodesView,
      props: true
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
