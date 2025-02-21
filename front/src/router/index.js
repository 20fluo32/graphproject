import Vue from 'vue'
import Router from 'vue-router'
import VideoRecognition from '../components/VideoRecognition'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'VideoRecognition',
      component: VideoRecognition
    }
  ]
})
