<template>
  <div class="video-recognition">
    <div class="left-column">
      <!-- 上传视频文件 -->
      <div class="file-upload">
        <label for="fileUpload" class="upload-label">选择视频文件：</label>
        <input type="file" id="fileUpload" @change="handleFileUpload" accept="video/*" class="file-input"/>
      </div>

      <!-- 选择检测的目标 -->
      <div v-if="file" class="labels-selection">
        <label for="labels" class="labels-label">选择要检测的目标：</label>
        <select v-model="selectedLabels" id="labels" multiple class="labels-select">
          <option v-for="(label, index) in availableLabels" :key="index" :value="index">
            {{ label }}
          </option>
        </select>
      </div>

      <!-- 检测按钮 -->
      <div v-if="file && selectedLabels.length > 0" class="action-btn">
        <button :disabled="isLoading" @click="startRecognition">
          {{ isLoading ? '处理中...' : '开始检测' }}
        </button>
      </div>
    </div>

    <div class="right-column">
      <!-- 播放处理完成的视频 -->
      <div v-if="outputVideoUrl" class="output-video">
        <h2>检测完成，播放处理后的视频：</h2>
        <video controls :src="outputVideoUrl" preload="auto" width="100%"></video>

        <!-- 下载按钮 -->
        <button v-if="outputVideoName" @click="downloadVideo" class="download-btn">
          下载视频
        </button>
      </div>

      <!-- 错误信息显示 -->
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  data () {
    return {
      file: null, // 上传的视频文件
      availableLabels: [
        '人', '自行车', '汽车', '摩托车', '飞机', '公共汽车', '火车', '卡车', '船',
        '交通灯', '消防栓', '停止标志', '停车收费表', '长椅', '鸟', '猫', '狗',
        '马', '羊', '牛', '大象', '熊', '斑马', '长颈鹿', '背包', '雨伞',
        '手提包', '领带', '手提箱', '飞盘', '滑雪板', '滑雪橇', '运动球', '风筝',
        '棒球棒', '棒球手套', '滑板', '冲浪板', '网球拍', '瓶子', '酒杯',
        '杯子', '叉子', '刀子', '勺子', '碗', '香蕉', '苹果', '三明治',
        '橙子', '西兰花', '胡萝卜', '热狗', '披萨', '甜甜圈', '蛋糕', '椅子', '沙发',
        '盆栽', '床', '餐桌', '马桶', '电视', '笔记本电脑', '鼠标', '遥控器',
        '键盘', '手机', '微波炉', '烤箱', '烤面包机', '水槽', '冰箱', '书',
        '钟表', '花瓶', '剪刀', '泰迪熊', '吹风机', '牙刷'
      ], // 标签列表
      selectedLabels: [], // 用户选择的标签（List<Integer>）
      outputVideoUrl: null, // 处理完成后的视频地址
      outputVideoName: null,
      isLoading: false, // 是否正在加载
      errorMessage: null // 错误信息
    }
  },
  methods: {
    // 处理视频文件上传
    handleFileUpload (event) {
      const file = event.target.files[0]
      if (file) {
        if (!file.type.startsWith('video/')) {
          alert('请选择有效的视频文件！')
          this.file = null
          return
        }
        this.file = file
        this.errorMessage = null // 清空错误信息

        // 保存文件到 localStorage
        localStorage.setItem('uploadedFile', JSON.stringify({ name: file.name, type: file.type }))
      }
    },

    // 处理检测目标选择
    handleLabelSelection () {
      // 保存选择的标签到 localStorage
      localStorage.setItem('selectedLabels', JSON.stringify(this.selectedLabels))
    },

    // 开始检测
    async startRecognition () {
      if (!this.file || this.selectedLabels.length === 0) {
        alert('请选择视频文件和检测目标！')
        return
      }

      this.isLoading = true
      this.errorMessage = null

      const formData = new FormData()
      formData.append('file', this.file)
      formData.append('labels', this.selectedLabels)

      try {
        const response = await axios.post('/video/recognize', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

        if (response.data) {
          this.outputVideoName = response.data
          this.outputVideoUrl = 'http://localhost:8080/stream/' + this.outputVideoName
        } else {
          this.errorMessage = '后端没有返回有效的视频路径，请检查后端服务。'
        }
      } catch (error) {
        console.error('视频处理失败:', error)
        this.errorMessage = '视频处理失败，请稍后重试！'
      } finally {
        this.isLoading = false
      }

      // 保存 loading 状态
      localStorage.setItem('isLoading', JSON.stringify(this.isLoading))
    },

    // 下载视频
    downloadVideo () {
      const link = document.createElement('a')
      link.href = this.outputVideoUrl
      link.download = this.outputVideoName
      // 设置 download 属性，并确保浏览器会进行下载而不是打开
      link.setAttribute('download', this.outputVideoName)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    }
  },

  mounted () {
    // 恢复之前保存的文件和选择的标签
    const savedFile = localStorage.getItem('uploadedFile')
    const savedLabels = localStorage.getItem('selectedLabels')
    const savedLoading = localStorage.getItem('isLoading')

    if (savedFile) {
      const fileData = JSON.parse(savedFile)
      this.file = fileData
    }

    if (savedLabels) {
      this.selectedLabels = JSON.parse(savedLabels)
    }

    if (savedLoading) {
      this.isLoading = JSON.parse(savedLoading)
    }
  },

  watch: {
    // 每次选中标签时保存状态
    selectedLabels (newLabels) {
      localStorage.setItem('selectedLabels', JSON.stringify(newLabels))
    }
  }
}
</script>

<style scoped>
.video-recognition {
  display: flex;
  justify-content: space-between;
  max-width: 1200px;
  margin: auto;
  padding: 20px;
  box-sizing: border-box;
}

.left-column,
.right-column {
  width: 48%;
}

.left-column {
  padding-right: 20px;
}

.right-column {
  padding-left: 20px;
}

.upload-label,
.labels-label {
  font-size: 16px;
  margin-bottom: 10px;
}

.file-input,
.labels-select {
  width: 100%;
  max-width: 300px;
  padding: 10px;
  margin: 10px 0;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.file-input:focus,
.labels-select:focus {
  border-color: #4caf50;
  outline: none;
}

.labels-select {
  height: 150px;
  overflow-y: auto;
}

button {
  padding: 12px 20px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  width: 100%;
  max-width: 300px;
  margin-top: 20px;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

button:hover:not(:disabled) {
  background-color: #45a049;
}

.output-video video {
  margin-top: 20px;
  max-width: 100%;
  border-radius: 8px;
}

.download-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #008CBA;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.download-btn:hover {
  background-color: #007b9e;
}

.error-message {
  color: red;
  margin-top: 20px;
  font-size: 14px;
}

@media (max-width: 768px) {
  .video-recognition {
    flex-direction: column;
  }

  .left-column,
  .right-column {
    width: 100%;
    padding: 0;
  }

  .file-input,
  .labels-select,
  button {
    width: 100%;
    max-width: none;
  }
}
</style>
