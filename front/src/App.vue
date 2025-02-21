<template>
  <div id="app">
    <h1>欢迎来到视频检测系统</h1>
    <!-- 路由视图 -->
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
/* 样式可以全局应用 */
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  text-align: center;
  color: #333;
}
</style>
