<template>
  <div class="price-prediction-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <button class="btn-back" @click="goBack">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1 class="page-title">农产品价格预测</h1>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 步骤1: 文件上传 -->
      <div v-if="step === 1" class="step-section">
        <h2 class="section-title">步骤1: 上传Excel文件</h2>
        <div class="upload-area" 
             :class="{ 'drag-over': isDragOver }"
             @drop="handleDrop"
             @dragover.prevent="isDragOver = true"
             @dragleave="isDragOver = false"
             @click="triggerFileInput">
          <input 
            ref="fileInput"
            type="file" 
            accept=".xls,.xlsx"
            @change="handleFileSelect"
            style="display: none"
          />
          <div class="upload-content">
            <div class="upload-icon">📊</div>
            <p class="upload-text">点击或拖拽Excel文件到此处上传</p>
            <p class="upload-hint">支持 .xls 和 .xlsx 格式，文件大小不超过10MB</p>
            <div class="format-example">
              <p><strong>Excel格式要求：</strong></p>
              <table class="example-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>价格</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>2024-01-01</td>
                    <td>10.5</td>
                  </tr>
                  <tr>
                    <td>2024-01-02</td>
                    <td>11.2</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        
        <div v-if="uploadedFile" class="file-info">
          <div class="file-item">
            <span class="file-name">{{ uploadedFile.name }}</span>
            <span class="file-size">({{ formatFileSize(uploadedFile.size) }})</span>
            <button class="btn-remove" @click="removeFile">移除</button>
          </div>
        </div>

        <div v-if="previewData && previewData.length > 0" class="preview-section">
          <h3 class="preview-title">数据预览（前{{ previewData.length }}条）</h3>
          <div class="preview-table-wrapper">
            <table class="preview-table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>价格</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in previewData" :key="index">
                  <td>{{ item.date }}</td>
                  <td>¥{{ item.price }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="preview-total">共 {{ totalRecords }} 条数据</p>
        </div>

        <div class="action-buttons">
          <button 
            class="btn-primary" 
            :disabled="!fileId || uploading"
            @click="proceedToStep2">
            {{ uploading ? '上传中...' : '下一步：设置预测参数' }}
          </button>
        </div>
      </div>

      <!-- 步骤2: 预测参数设置 -->
      <div v-if="step === 2" class="step-section">
        <h2 class="section-title">步骤2: 设置预测参数</h2>
        
        <div class="form-group">
          <label class="form-label">预测天数</label>
          <input 
            type="number" 
            v-model.number="predictionDays"
            min="1"
            max="90"
            class="form-input"
          />
          <p class="form-hint">预测未来多少天的价格（1-90天）</p>
        </div>

         <div class="form-group">
           <label class="form-label">预测模型</label>
           <div class="model-info-box">
             <div class="model-badge">时间序列模型（指数平滑）</div>
             <p class="model-description">
               系统使用时间序列模型进行预测，该模型通过指数平滑算法自动学习数据规律，
               能够有效捕捉价格的时间依赖关系和趋势变化，预测准确度较高。
             </p>
           </div>
         </div>

        <div class="action-buttons">
          <button class="btn-secondary" @click="step = 1">上一步</button>
          <button 
            class="btn-primary" 
            :disabled="predicting"
            @click="startPrediction">
            {{ predicting ? '预测中...' : '开始预测' }}
          </button>
        </div>
      </div>

      <!-- 步骤3: 预测结果 -->
      <div v-if="step === 3" class="step-section">
        <h2 class="section-title">预测结果</h2>
        
        <div v-if="predictionResult" class="result-section">
          <!-- 模型评估指标 -->
          <div class="metrics-card">
            <h3 class="metrics-title">模型评估指标</h3>
            <div class="metrics-grid">
              <div class="metric-item">
                <div class="metric-label">R²决定系数</div>
                <div class="metric-value">{{ predictionResult.model_metrics.r_squared.toFixed(4) }}</div>
                <div class="metric-desc">越接近1越好</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">平均绝对误差(MAE)</div>
                <div class="metric-value">{{ predictionResult.model_metrics.mae.toFixed(2) }}</div>
                <div class="metric-desc">越小越好</div>
              </div>
              <div class="metric-item">
                <div class="metric-label">均方根误差(RMSE)</div>
                <div class="metric-value">{{ predictionResult.model_metrics.rmse.toFixed(2) }}</div>
                <div class="metric-desc">越小越好</div>
              </div>
            </div>
          </div>

          <!-- 趋势分析 -->
          <div class="trend-card">
            <h3 class="trend-title">价格趋势</h3>
            <div class="trend-badge" :class="getTrendClass(predictionResult.trend)">
              {{ getTrendText(predictionResult.trend) }}
            </div>
          </div>

          <!-- 图表展示 -->
          <div class="chart-card">
            <h3 class="chart-title">价格走势图</h3>
            <div class="chart-container" ref="chartContainer"></div>
          </div>

          <!-- 预测数据表格 -->
          <div class="data-card">
            <h3 class="data-title">预测数据</h3>
            <div class="data-table-wrapper">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>预测价格</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in predictionResult.predicted_data" :key="index">
                    <td>{{ item.date }}</td>
                    <td>¥{{ item.price }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <button class="btn-secondary" @click="reset">重新预测</button>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </div>
    </main>
  </div>
</template>

<script>
import { ref, onMounted, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { pricePredictionService } from '../api/pricePrediction';
import logger from '../utils/logger';

export default {
  name: 'PricePrediction',
  setup() {
    const router = useRouter();
    const fileInput = ref(null);
    const chartContainer = ref(null);
    
    const step = ref(1);
    const isDragOver = ref(false);
    const uploadedFile = ref(null);
    const fileId = ref(null);
    const previewData = ref([]);
    const totalRecords = ref(0);
    const uploading = ref(false);
    const predicting = ref(false);
    const predictionDays = ref(30);
    const modelType = ref('timeseries'); // 固定使用时间序列模型
    const predictionResult = ref(null);
    const errorMessage = ref('');

    // 返回上一页
    const goBack = () => {
      router.push('/home');
    };

    // 触发文件选择
    const triggerFileInput = () => {
      fileInput.value?.click();
    };

    // 处理文件选择
    const handleFileSelect = (event) => {
      const file = event.target.files[0];
      if (file) {
        processFile(file);
      }
    };

    // 处理拖拽
    const handleDrop = (event) => {
      event.preventDefault();
      isDragOver.value = false;
      const file = event.dataTransfer.files[0];
      if (file) {
        processFile(file);
      }
    };

    // 处理文件
    const processFile = async (file) => {
      // 验证文件类型
      if (!file.name.endsWith('.xls') && !file.name.endsWith('.xlsx')) {
        errorMessage.value = '不支持的文件格式，仅支持.xls和.xlsx文件';
        return;
      }

      // 验证文件大小（10MB）
      if (file.size > 10 * 1024 * 1024) {
        errorMessage.value = '文件大小不能超过10MB';
        return;
      }

      errorMessage.value = '';
      uploadedFile.value = file;
      
      // 上传文件
      uploading.value = true;
      try {
        const result = await pricePredictionService.uploadExcel(file);
        fileId.value = result.file_id;
        previewData.value = result.preview_data || [];
        totalRecords.value = result.total_records || 0;
        logger.info('PRICE_PREDICTION', '文件上传成功', { fileId: fileId.value, totalRecords: totalRecords.value });
      } catch (error) {
        errorMessage.value = error.message || '上传文件失败';
        logger.error('PRICE_PREDICTION', '文件上传失败', {}, error);
      } finally {
        uploading.value = false;
      }
    };

    // 移除文件
    const removeFile = () => {
      uploadedFile.value = null;
      fileId.value = null;
      previewData.value = [];
      totalRecords.value = 0;
      if (fileInput.value) {
        fileInput.value.value = '';
      }
    };

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    };

    // 进入下一步
    const proceedToStep2 = () => {
      if (fileId.value) {
        step.value = 2;
      }
    };

    // 开始预测
    const startPrediction = async () => {
      if (!fileId.value) {
        errorMessage.value = '请先上传Excel文件';
        return;
      }

      errorMessage.value = '';
      predicting.value = true;

      try {
        const result = await pricePredictionService.predictPrice(
          fileId.value,
          predictionDays.value,
          modelType.value
        );
        predictionResult.value = result;
        step.value = 3;
        
        // 等待DOM更新后绘制图表
        await nextTick();
        drawChart();
        
        logger.info('PRICE_PREDICTION', '价格预测成功', { 
          predictionDays: predictionDays.value,
          modelType: modelType.value 
        });
      } catch (error) {
        errorMessage.value = error.message || '预测失败';
        logger.error('PRICE_PREDICTION', '价格预测失败', {}, error);
      } finally {
        predicting.value = false;
      }
    };

    // 绘制图表（使用简单的Canvas绘制）
    const drawChart = () => {
      if (!chartContainer.value || !predictionResult.value) return;

      const container = chartContainer.value;
      const canvas = document.createElement('canvas');
      canvas.width = container.clientWidth;
      canvas.height = 400;
      container.innerHTML = '';
      container.appendChild(canvas);

      const ctx = canvas.getContext('2d');
      const padding = 60;
      const chartWidth = canvas.width - padding * 2;
      const chartHeight = canvas.height - padding * 2;

      // 合并历史数据和预测数据
      const allData = [
        ...predictionResult.value.historical_data.map(d => ({ ...d, type: 'historical' })),
        ...predictionResult.value.predicted_data.map(d => ({ ...d, type: 'predicted' }))
      ];

      // 找到价格的最大值和最小值
      const prices = allData.map(d => d.price);
      const minPrice = Math.min(...prices);
      const maxPrice = Math.max(...prices);
      const priceRange = maxPrice - minPrice || 1;

      // 绘制坐标轴
      ctx.strokeStyle = '#ccc';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(padding, padding);
      ctx.lineTo(padding, canvas.height - padding);
      ctx.lineTo(canvas.width - padding, canvas.height - padding);
      ctx.stroke();

      // 绘制历史数据
      ctx.strokeStyle = '#4CAF50';
      ctx.lineWidth = 2;
      ctx.beginPath();
      const historicalData = predictionResult.value.historical_data;
      historicalData.forEach((point, index) => {
        const x = padding + (index / (historicalData.length - 1)) * chartWidth;
        const y = canvas.height - padding - ((point.price - minPrice) / priceRange) * chartHeight;
        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.stroke();

      // 绘制预测数据
      ctx.strokeStyle = '#FF9800';
      ctx.lineWidth = 2;
      ctx.setLineDash([5, 5]);
      ctx.beginPath();
      const predictedData = predictionResult.value.predicted_data;
      const startX = padding + chartWidth;
      predictedData.forEach((point, index) => {
        const x = startX + (index / (predictedData.length - 1)) * chartWidth;
        const y = canvas.height - padding - ((point.price - minPrice) / priceRange) * chartHeight;
        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.stroke();
      ctx.setLineDash([]);

      // 绘制分隔线
      ctx.strokeStyle = '#999';
      ctx.lineWidth = 1;
      ctx.setLineDash([2, 2]);
      ctx.beginPath();
      ctx.moveTo(startX, padding);
      ctx.lineTo(startX, canvas.height - padding);
      ctx.stroke();
      ctx.setLineDash([]);

      // 添加图例
      ctx.fillStyle = '#4CAF50';
      ctx.fillRect(canvas.width - 150, 20, 15, 15);
      ctx.fillStyle = '#333';
      ctx.font = '12px Arial';
      ctx.fillText('历史数据', canvas.width - 130, 32);

      ctx.fillStyle = '#FF9800';
      ctx.fillRect(canvas.width - 150, 40, 15, 15);
      ctx.fillStyle = '#333';
      ctx.fillText('预测数据', canvas.width - 130, 52);
    };

    // 获取趋势文本
    const getTrendText = (trend) => {
      const trendMap = {
        '上升': '📈 上升趋势',
        '下降': '📉 下降趋势',
        '平稳': '➡️ 平稳趋势',
        '波动': '📊 波动趋势'
      };
      return trendMap[trend] || trend;
    };

    // 获取趋势样式类
    const getTrendClass = (trend) => {
      const classMap = {
        '上升': 'trend-up',
        '下降': 'trend-down',
        '平稳': 'trend-stable',
        '波动': 'trend-fluctuate'
      };
      return classMap[trend] || '';
    };

    // 重置
    const reset = () => {
      step.value = 1;
      uploadedFile.value = null;
      fileId.value = null;
      previewData.value = [];
      totalRecords.value = 0;
      predictionResult.value = null;
      errorMessage.value = '';
       predictionDays.value = 30;
       modelType.value = 'timeseries';
      if (fileInput.value) {
        fileInput.value.value = '';
      }
    };

    return {
      fileInput,
      chartContainer,
      step,
      isDragOver,
      uploadedFile,
      fileId,
      previewData,
      totalRecords,
      uploading,
      predicting,
      predictionDays,
      modelType,
      predictionResult,
      errorMessage,
      goBack,
      triggerFileInput,
      handleFileSelect,
      handleDrop,
      removeFile,
      formatFileSize,
      proceedToStep2,
      startPrediction,
      reset,
      getTrendText,
      getTrendClass
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.price-prediction-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}

.header {
  background: var(--white);
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  color: var(--gray-600);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--gray-100);
  border-color: var(--primary);
  color: var(--primary);
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

.main-content {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.step-section {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a202c;
  margin-bottom: 2rem;
}

.upload-area {
  border: 2px dashed var(--gray-300);
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: var(--gray-50);
}

.upload-area:hover,
.upload-area.drag-over {
  border-color: var(--primary);
  background: var(--primary-light);
}

.upload-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.upload-text {
  font-size: 1.125rem;
  font-weight: 500;
  color: #1a202c;
  margin-bottom: 0.5rem;
}

.upload-hint {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin-bottom: 2rem;
}

.format-example {
  margin-top: 2rem;
  text-align: left;
  display: inline-block;
}

.example-table {
  border-collapse: collapse;
  margin-top: 0.5rem;
}

.example-table th,
.example-table td {
  border: 1px solid var(--gray-300);
  padding: 0.5rem 1rem;
  text-align: left;
}

.example-table th {
  background: var(--gray-100);
  font-weight: 600;
}

.file-info {
  margin-top: 1.5rem;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--gray-50);
  border-radius: 8px;
}

.file-name {
  font-weight: 500;
  color: #1a202c;
}

.file-size {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.btn-remove {
  margin-left: auto;
  padding: 0.25rem 0.75rem;
  background: var(--error);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
}

.preview-section {
  margin-top: 2rem;
}

.preview-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.preview-table-wrapper {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
}

.preview-table th,
.preview-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid var(--gray-200);
}

.preview-table th {
  background: var(--gray-100);
  font-weight: 600;
  position: sticky;
  top: 0;
}

.preview-total {
  margin-top: 0.5rem;
  color: var(--gray-500);
  font-size: 0.875rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  font-weight: 500;
  margin-bottom: 0.5rem;
  color: #1a202c;
}

.form-input,
.form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 1rem;
}

 .form-hint {
   margin-top: 0.25rem;
   font-size: 0.875rem;
   color: var(--gray-500);
 }

 .model-info-box {
   padding: 1rem;
   background: var(--gray-50);
   border-radius: 8px;
   border: 1px solid var(--gray-200);
 }

 .model-badge {
   display: inline-block;
   padding: 0.5rem 1rem;
   background: var(--primary);
   color: white;
   border-radius: 6px;
   font-weight: 500;
   margin-bottom: 0.75rem;
 }

 .model-description {
   font-size: 0.875rem;
   color: var(--gray-600);
   line-height: 1.6;
   margin: 0;
 }

.action-buttons {
  display: flex;
  gap: 1rem;
  margin-top: 2rem;
}

.btn-primary {
  flex: 1;
  padding: 0.75rem 1.5rem;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 0.75rem 1.5rem;
  background: var(--gray-200);
  color: #1a202c;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: var(--gray-300);
}

.metrics-card,
.trend-card,
.chart-card,
.data-card {
  background: var(--gray-50);
  padding: 1.5rem;
  border-radius: 12px;
  margin-bottom: 1.5rem;
}

.metrics-title,
.trend-title,
.chart-title,
.data-title {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
}

.metric-item {
  text-align: center;
  padding: 1rem;
  background: white;
  border-radius: 8px;
}

.metric-label {
  font-size: 0.875rem;
  color: var(--gray-500);
  margin-bottom: 0.5rem;
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 0.25rem;
}

.metric-desc {
  font-size: 0.75rem;
  color: var(--gray-400);
}

.trend-badge {
  display: inline-block;
  padding: 0.75rem 1.5rem;
  border-radius: 8px;
  font-size: 1.125rem;
  font-weight: 500;
}

.trend-up {
  background: #d1fae5;
  color: #065f46;
}

.trend-down {
  background: #fee2e2;
  color: #991b1b;
}

.trend-stable {
  background: #e0e7ff;
  color: #3730a3;
}

.trend-fluctuate {
  background: #fef3c7;
  color: #92400e;
}

.chart-container {
  width: 100%;
  height: 400px;
  background: white;
  border-radius: 8px;
  padding: 1rem;
}

.data-table-wrapper {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid var(--gray-200);
}

.data-table th {
  background: var(--gray-100);
  font-weight: 600;
  position: sticky;
  top: 0;
}

.error-message {
  margin-top: 1rem;
  padding: 1rem;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 8px;
  border: 1px solid #fecaca;
}

@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>

