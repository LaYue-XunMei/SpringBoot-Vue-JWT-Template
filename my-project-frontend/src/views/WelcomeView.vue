<script setup>

</script>

<template>
  <div style="width: 100vw; height: 100vh;overflow: hidden;display: flex;">
    <div style="flex:1;background-color: black">
      <el-image style="width: 100%; height: 100%" fit="cover"
                src="src/pic/sky.jpg"/>

    </div>

    <div class="welcome-title" >
      <div style="font-size: 30px;font-weight: bold">欢迎来到学习平台</div>
      <div style="font-size: 20px;margin-top: 10px">在这里学习开发技能</div>
      <div style="font-size: 20px;margin-top: 5px">网站维护知识</div>
    </div>

    <div class="right-card">
      <router-view/>
    </div>


  </div>

</template>

<style scoped>
.right-card {
  width: 400px;
  z-index: 1;
  background-color: white;
}

.welcome-title{
  position: absolute;
  bottom: 30px;
  left: 30px;
  color: aliceblue;
}

</style>