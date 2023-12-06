import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ListPodcastsView from '../views/ListPodcastsView.vue'
import CreatePodcastView from '@/views/CreatePodcastView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/list-podcasts',
      name: 'list-podcasts',
      component: ListPodcastsView
    },
    {
      path: '/create-podcast',
      name: 'create-podcast',
      component: CreatePodcastView
    }
  ]
})

export default router
