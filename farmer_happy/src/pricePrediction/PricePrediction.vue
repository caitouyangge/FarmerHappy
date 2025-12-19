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
        <h2 class="section-title">步骤1: 上传价格文件（Excel/CSV）</h2>
        <div class="upload-area" 
             :class="{ 'drag-over': isDragOver }"
             @drop="handleDrop"
             @dragover.prevent="isDragOver = true"
             @dragleave="isDragOver = false"
             @click="triggerFileInput">
          <input 
            ref="fileInput"
            type="file" 
            accept=".xls,.xlsx,.csv"
            @change="handleFileSelect"
            style="display: none"
          />
          <div class="upload-content">
            <div class="upload-icon">📊</div>
            <p class="upload-text">点击或拖拽文件到此处上传</p>
            <p class="upload-hint">支持 .xls / .xlsx / .csv，文件大小不超过10MB</p>
            <div class="format-example">
              <p><strong>文件格式要求（任一即可）：</strong></p>
              <table class="example-table">
                <thead>
                  <tr>
                    <th>规格/类型</th>
                    <th>平均价</th>
                    <th>发布日期</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>活</td>
                    <td>45.0</td>
                    <td>2025-12-18</td>
                  </tr>
                  <tr>
                    <td>冰鲜</td>
                    <td>105.0</td>
                    <td>2025-12-17</td>
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
                  <th>规格/类型</th>
                  <th>日期</th>
                  <th>价格</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in previewData" :key="index">
                  <td>{{ item.type || '默认' }}</td>
                  <td>{{ item.date }}</td>
                  <td>¥{{ item.price }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="preview-total">
            <strong>共 {{ totalRecords }} 条数据</strong>
            <span class="preview-note">（预览仅显示前{{ previewData.length }}条，预测时将处理全部 {{ totalRecords }} 条数据）</span>
          </p>
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
            <div class="model-badge">ARIMA模型（自回归综合移动平均模型）</div>
             <p class="model-description">
              系统使用<strong>ARIMA（AutoRegressive Integrated Moving Average）</strong>自回归综合移动平均模型进行价格预测。
              ARIMA模型是时间序列分析中最经典和广泛使用的模型之一，特别适合分析具有趋势和季节性的时间序列数据。
              模型通过<strong>自回归(AR)</strong>捕捉历史值对当前值的影响，通过<strong>差分(I)</strong>处理非平稳性，
              通过<strong>移动平均(MA)</strong>捕捉误差项的影响。对于具有季节性的数据，系统会自动检测并应用<strong>SARIMA（季节性ARIMA）</strong>模型。
              系统会基于留出集回测（holdout）自动选择最优的参数组合（p, d, q），并保留"持平外推(naive)"作为基线对比。
              ARIMA模型能够更准确地捕捉时间序列的内在规律，提供更可靠的预测结果。
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
              <div v-if="predictionResult.model_metrics.mape !== undefined" class="metric-item">
                <div class="metric-label">平均百分比误差(MAPE)</div>
                <div class="metric-value">{{ (predictionResult.model_metrics.mape * 100).toFixed(2) }}%</div>
                <div class="metric-desc">越小越好</div>
              </div>
              <div v-if="predictionResult.model_metrics.aic" class="metric-item">
                <div class="metric-label">AIC信息准则</div>
                <div class="metric-value">{{ predictionResult.model_metrics.aic.toFixed(2) }}</div>
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

          <!-- 详细计算过程 -->
          <div class="calculation-card">
            <h3 class="calculation-title" @click="toggleCalculationDetails">
              <span>详细计算过程</span>
              <span class="toggle-icon">{{ showCalculationDetails ? '▼' : '▶' }}</span>
            </h3>
            
            <div v-if="showCalculationDetails" class="calculation-content">
              <div v-if="!predictionResult.calculation_details" class="calculation-info">
                <p style="color: var(--gray-500);">计算详情正在加载中...</p>
              </div>
              
              <template v-else>
                <!-- 数据预处理 -->
                <div v-if="predictionResult.calculation_details.preprocessing" class="calculation-section">
                  <h4 class="section-subtitle">1. 数据预处理</h4>
                  <div class="calculation-info">
                    <p><strong>原始数据点数量：</strong>{{ predictionResult.calculation_details.preprocessing.original_count }}</p>
                    <p><strong>清洗后数据点数量：</strong>{{ predictionResult.calculation_details.preprocessing.cleaned_count }}</p>
                    <p v-if="predictionResult.calculation_details.preprocessing.filled_count !== undefined">
                      <strong>补齐缺失日期后点数：</strong>{{ predictionResult.calculation_details.preprocessing.filled_count }}
                    </p>
                    <p v-if="predictionResult.calculation_details.preprocessing.removed_count > 0">
                      <strong>去除异常值数量：</strong>{{ predictionResult.calculation_details.preprocessing.removed_count }}
                    </p>
                    <p v-if="predictionResult.calculation_details.preprocessing.method">
                      <strong>处理方法：</strong>{{ predictionResult.calculation_details.preprocessing.method }}
                    </p>
                    <div v-if="predictionResult.calculation_details.preprocessing.mean" class="formula-box">
                      <p><strong>均值：</strong>{{ predictionResult.calculation_details.preprocessing.mean }}</p>
                      <p><strong>标准差：</strong>{{ predictionResult.calculation_details.preprocessing.std_dev }}</p>
                      <p><strong>下界（均值 - 3×标准差）：</strong>{{ predictionResult.calculation_details.preprocessing.lower_bound }}</p>
                      <p><strong>上界（均值 + 3×标准差）：</strong>{{ predictionResult.calculation_details.preprocessing.upper_bound }}</p>
                    </div>
                  </div>
                </div>

                <!-- 模型选择（回测） -->
                <div v-if="predictionResult.calculation_details.model_selection" class="calculation-section">
                  <h4 class="section-subtitle">2. 模型选择（留出集回测）</h4>
                  <div class="calculation-info">
                    <p><strong>最终采用模型：</strong>{{ predictionResult.calculation_details.model_selection.model_name }}</p>
                    <p><strong>选择方法：</strong>{{ predictionResult.calculation_details.model_selection.selection_method }}</p>
                    <div class="formula-box">
                      <p><strong>季节周期：</strong>{{ predictionResult.calculation_details.model_selection.season_length }} 天</p>
                      <p><strong>参数(α/β/γ)：</strong>
                        {{ predictionResult.calculation_details.model_selection.alpha }},
                        {{ predictionResult.calculation_details.model_selection.beta }},
                        {{ predictionResult.calculation_details.model_selection.gamma }}
                      </p>
                      <p v-if="predictionResult.calculation_details.model_selection.phi !== undefined">
                        <strong>阻尼趋势(φ)：</strong>{{ predictionResult.calculation_details.model_selection.phi }}
                      </p>
                      <p v-if="predictionResult.calculation_details.model_selection.psi !== undefined">
                        <strong>季节衰减(ψ)：</strong>{{ predictionResult.calculation_details.model_selection.psi }}
                      </p>
                      <p v-if="predictionResult.calculation_details.model_selection.seasonality_strength !== undefined">
                        <strong>季节性强度：</strong>{{ predictionResult.calculation_details.model_selection.seasonality_strength }}
                      </p>
                      <p v-if="predictionResult.calculation_details.model_selection.forecast_strategy">
                        <strong>预测策略：</strong>{{ predictionResult.calculation_details.model_selection.forecast_strategy }}
                      </p>
                      <p v-if="predictionResult.calculation_details.model_selection.cv_folds !== undefined">
                        <strong>回测折数：</strong>{{ predictionResult.calculation_details.model_selection.cv_folds }}
                      </p>
                      <p><strong>回测留出集大小：</strong>{{ predictionResult.calculation_details.model_selection.holdout_size }}</p>
                    </div>
                    <div v-if="predictionResult.calculation_details.model_selection.holdout_metrics" class="formula-box">
                      <p><strong>回测指标（选中模型）：</strong></p>
                      <p>MAE = {{ predictionResult.calculation_details.model_selection.holdout_metrics.mae }}</p>
                      <p>RMSE = {{ predictionResult.calculation_details.model_selection.holdout_metrics.rmse }}</p>
                      <p>MAPE = {{ (predictionResult.calculation_details.model_selection.holdout_metrics.mape * 100).toFixed(2) }}%</p>
                      <p>R² = {{ predictionResult.calculation_details.model_selection.holdout_metrics.r_squared }}</p>
                    </div>
                    <div v-if="predictionResult.calculation_details.model_selection.baseline_metrics" class="formula-box">
                      <p><strong>基线指标（Naive持平外推）：</strong></p>
                      <p>MAE = {{ predictionResult.calculation_details.model_selection.baseline_metrics.mae }}</p>
                      <p>RMSE = {{ predictionResult.calculation_details.model_selection.baseline_metrics.rmse }}</p>
                      <p>MAPE = {{ (predictionResult.calculation_details.model_selection.baseline_metrics.mape * 100).toFixed(2) }}%</p>
                      <p>R² = {{ predictionResult.calculation_details.model_selection.baseline_metrics.r_squared }}</p>
                    </div>
                  </div>
                </div>

                <!-- 预测过程 -->
                <div v-if="predictionResult.calculation_details.prediction_steps" class="calculation-section">
                  <h4 class="section-subtitle">3. 预测计算过程</h4>
                  <div class="calculation-info">
                    <p class="formula-intro">
                      <strong>说明：</strong>系统展示每一步预测的日期、公式提示与预测结果。若采用ARIMA模型，会根据选定的参数(p, d, q)进行预测；
                      若采用Naive，则为"持平外推"。
                    </p>
                    <div class="table-controls">
                      <button
                        v-if="predictionResult.calculation_details.prediction_steps.length > 20"
                        @click="showAllPredictionSteps = !showAllPredictionSteps"
                        class="btn-toggle-table">
                        {{ showAllPredictionSteps ? '收起' : '展开全部' }}（共{{ predictionResult.calculation_details.prediction_steps.length }}条）
                      </button>
                    </div>
                    <div class="calculation-table-wrapper">
                      <table class="calculation-table">
                        <thead>
                          <tr>
                            <th>日期</th>
                            <th>步数</th>
                            <th>计算公式</th>
                            <th>预测价格</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(step, index) in (showAllPredictionSteps ? predictionResult.calculation_details.prediction_steps : predictionResult.calculation_details.prediction_steps.slice(0, 20))" :key="index">
                            <td>{{ step.date }}</td>
                            <td>{{ step.step }}</td>
                            <td class="formula-cell">{{ step.formula }}</td>
                            <td><strong>¥{{ step.predicted_price }}</strong></td>
                          </tr>
                        </tbody>
                      </table>
                      <p v-if="!showAllPredictionSteps && predictionResult.calculation_details.prediction_steps.length > 20" class="table-note">
                        显示前20条，共{{ predictionResult.calculation_details.prediction_steps.length }}条预测数据，点击"展开全部"查看完整数据
                      </p>
                    </div>
                  </div>
                </div>
              </template>
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
    const showCalculationDetails = ref(false);
    const showAllSmoothingSteps = ref(false);
    const showAllEvaluationSteps = ref(false);
    const showAllPredictionSteps = ref(false);

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
      const lowerName = (file.name || '').toLowerCase();
      if (!lowerName.endsWith('.xls') && !lowerName.endsWith('.xlsx') && !lowerName.endsWith('.csv')) {
        errorMessage.value = '不支持的文件格式，仅支持 .xls / .xlsx / .csv 文件';
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
        errorMessage.value = '请先上传文件';
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
        
        // 调试：输出计算结果
        console.log('预测结果:', result);
        console.log('计算详情:', result.calculation_details);
        
        step.value = 3;
        
        // 等待DOM更新后绘制图表
        await nextTick();
        drawChart();
        
        logger.info('PRICE_PREDICTION', '价格预测成功', { 
          predictionDays: predictionDays.value,
          modelType: modelType.value,
          hasCalculationDetails: !!result.calculation_details
        });
      } catch (error) {
        errorMessage.value = error.message || '预测失败';
        logger.error('PRICE_PREDICTION', '价格预测失败', {}, error);
      } finally {
        predicting.value = false;
      }
    };

    // 绘制图表（使用简单的Canvas绘制）：支持按规格/类型绘制多条曲线，并标注
    const drawChart = () => {
      if (!chartContainer.value || !predictionResult.value) return;

      const container = chartContainer.value;
      const canvas = document.createElement('canvas');
      canvas.width = container.clientWidth;
      canvas.height = 420;
      container.innerHTML = '';
      container.appendChild(canvas);

      const ctx = canvas.getContext('2d');
      const padding = 60;
      const chartWidth = canvas.width - padding * 2;
      const chartHeight = canvas.height - padding * 2;

      // 统一成多序列结构
      const seriesList = Array.isArray(predictionResult.value.series_data) && predictionResult.value.series_data.length > 0
        ? predictionResult.value.series_data
        : [{
            type: '默认',
            historical_data: predictionResult.value.historical_data || [],
            predicted_data: predictionResult.value.predicted_data || []
          }];

      // 汇总全量点（用于确定坐标轴范围）
      const allPoints = [];
      seriesList.forEach(s => {
        (s.historical_data || []).forEach(p => allPoints.push({ ...p, __kind: 'historical', __type: s.type }));
        (s.predicted_data || []).forEach(p => allPoints.push({ ...p, __kind: 'predicted', __type: s.type }));
      });
      if (allPoints.length === 0) return;

      // 统一X轴：按日期去重排序
      const dateSet = new Set(allPoints.map(p => p.date));
      const dates = Array.from(dateSet).sort((a, b) => new Date(a) - new Date(b));
      const dateIndex = new Map(dates.map((d, i) => [d, i]));
      const xCount = dates.length;

      // 找到价格的最大值和最小值
      const prices = allPoints.map(d => d.price);
      const minPrice = Math.min(...prices);
      const maxPrice = Math.max(...prices);
      const priceRange = maxPrice - minPrice || 1;

      // 工具函数：映射坐标
      const xOf = (dateStr) => {
        const idx = dateIndex.get(dateStr);
        if (idx == null) return padding;
        if (xCount <= 1) return padding + chartWidth / 2;
        return padding + (idx / (xCount - 1)) * chartWidth;
      };
      const yOf = (price) => canvas.height - padding - ((price - minPrice) / priceRange) * chartHeight;

      // 绘制坐标轴
      ctx.strokeStyle = '#ccc';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(padding, padding);
      ctx.lineTo(padding, canvas.height - padding);
      ctx.lineTo(canvas.width - padding, canvas.height - padding);
      ctx.stroke();

      // 颜色盘
      const palette = ['#4CAF50', '#2196F3', '#9C27B0', '#FF5722', '#009688', '#795548', '#607D8B', '#E91E63'];

      // 画网格/刻度（简单版）
      ctx.fillStyle = '#666';
      ctx.font = '12px Arial';
      ctx.textAlign = 'right';
      ctx.textBaseline = 'middle';
      const gridLines = 5;
      for (let i = 0; i <= gridLines; i++) {
        const v = minPrice + (priceRange * i) / gridLines;
        const y = yOf(v);
        ctx.strokeStyle = '#eee';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(padding, y);
        ctx.lineTo(canvas.width - padding, y);
        ctx.stroke();
        ctx.fillText(`¥${v.toFixed(2)}`, padding - 8, y);
      }

      // X轴日期（抽样显示）
      ctx.textAlign = 'center';
      ctx.textBaseline = 'top';
      const xLabelCount = Math.min(6, dates.length);
      for (let i = 0; i < xLabelCount; i++) {
        const idx = Math.round((i / (xLabelCount - 1 || 1)) * (dates.length - 1));
        const d = dates[idx];
        ctx.fillStyle = '#666';
        ctx.fillText(d, xOf(d), canvas.height - padding + 10);
      }

      // 计算“历史-预测分界线”：取所有序列历史最后日期的最大值
      let dividerDate = null;
      seriesList.forEach(s => {
        const hist = s.historical_data || [];
        if (hist.length === 0) return;
        const last = hist[hist.length - 1].date;
        if (!dividerDate || new Date(last) > new Date(dividerDate)) dividerDate = last;
      });
      if (dividerDate && dateIndex.has(dividerDate)) {
        const dividerX = xOf(dividerDate);
        ctx.strokeStyle = '#999';
        ctx.lineWidth = 1.5;
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(dividerX, padding);
        ctx.lineTo(dividerX, canvas.height - padding);
        ctx.stroke();
        ctx.setLineDash([]);
      }

      // 绘制每条规格曲线：历史(实线) + 预测(虚线)，并在末尾标注规格名
      const legendX = canvas.width - padding + 10;
      let legendY = 20;
      const usedLabelYs = [];

      const placeEndLabel = (x, y, text, color) => {
        // 简单避让：与已有label y距离太近则下移
        let yy = y;
        for (let guard = 0; guard < 20; guard++) {
          if (usedLabelYs.every(v => Math.abs(v - yy) > 12)) break;
          yy += 12;
          if (yy > canvas.height - padding) yy = y - 12;
        }
        usedLabelYs.push(yy);
        ctx.fillStyle = color;
        ctx.font = '12px Arial';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        ctx.fillText(text, Math.min(x + 6, canvas.width - padding + 5), yy);
      };

      seriesList.forEach((s, idx) => {
        const color = palette[idx % palette.length];
        const typeName = s.type || '默认';
        const hist = (s.historical_data || []).slice().sort((a, b) => new Date(a.date) - new Date(b.date));
        const pred = (s.predicted_data || []).slice().sort((a, b) => new Date(a.date) - new Date(b.date));

        // legend（右侧）
        ctx.fillStyle = color;
        ctx.fillRect(legendX, legendY, 12, 12);
        ctx.fillStyle = '#333';
        ctx.font = '12px Arial';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'top';
        ctx.fillText(typeName, legendX + 18, legendY - 1);
        legendY += 18;

        // 历史线
        if (hist.length > 0) {
          ctx.strokeStyle = color;
          ctx.lineWidth = 2.2;
          ctx.setLineDash([]);
          ctx.beginPath();
          hist.forEach((p, i) => {
            const x = xOf(p.date);
            const y = yOf(p.price);
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
          });
          ctx.stroke();

          // 点
          ctx.fillStyle = color;
          hist.forEach((p) => {
            const x = xOf(p.date);
            const y = yOf(p.price);
            ctx.beginPath();
            ctx.arc(x, y, 2.6, 0, Math.PI * 2);
            ctx.fill();
          });
        }

        // 预测线（从历史最后点延伸）
        if (pred.length > 0) {
          ctx.strokeStyle = color;
          ctx.lineWidth = 2.2;
          ctx.setLineDash([7, 4]);
          ctx.beginPath();
          if (hist.length > 0) {
            const lastH = hist[hist.length - 1];
            ctx.moveTo(xOf(lastH.date), yOf(lastH.price));
          } else {
            ctx.moveTo(xOf(pred[0].date), yOf(pred[0].price));
          }
          pred.forEach((p) => ctx.lineTo(xOf(p.date), yOf(p.price)));
          ctx.stroke();
          ctx.setLineDash([]);

          ctx.fillStyle = color;
          pred.forEach((p) => {
            const x = xOf(p.date);
            const y = yOf(p.price);
            ctx.beginPath();
            ctx.arc(x, y, 2.6, 0, Math.PI * 2);
            ctx.fill();
          });
        }

        // 末尾标注（优先预测末尾，否则历史末尾）
        const endPoint = pred.length > 0 ? pred[pred.length - 1] : (hist.length > 0 ? hist[hist.length - 1] : null);
        if (endPoint) {
          placeEndLabel(xOf(endPoint.date), yOf(endPoint.price), typeName, color);
        }
      });
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

    // 切换详细计算过程显示
    const toggleCalculationDetails = () => {
      showCalculationDetails.value = !showCalculationDetails.value;
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
      showCalculationDetails.value = false;
      showAllSmoothingSteps.value = false;
      showAllEvaluationSteps.value = false;
      showAllPredictionSteps.value = false;
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
      showCalculationDetails,
      showAllSmoothingSteps,
      showAllEvaluationSteps,
      showAllPredictionSteps,
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
      getTrendClass,
      toggleCalculationDetails
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
  color: var(--gray-600);
  font-size: 0.875rem;
  line-height: 1.6;
}

.preview-note {
  display: block;
  margin-top: 0.25rem;
  color: var(--primary);
  font-size: 0.8125rem;
  font-weight: 500;
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
.data-card,
.calculation-card {
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

.calculation-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  padding: 0.5rem 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--primary);
  transition: color 0.2s;
}

.calculation-title:hover {
  color: var(--primary-dark);
}

.toggle-icon {
  font-size: 0.875rem;
  transition: transform 0.2s;
}

.calculation-content {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--gray-300);
}

.calculation-section {
  margin-bottom: 2rem;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  border-left: 4px solid var(--primary);
}

.section-subtitle {
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary);
  margin-bottom: 0.75rem;
}

.calculation-info {
  font-size: 0.875rem;
  line-height: 1.8;
  color: var(--gray-700);
}

.calculation-info p {
  margin: 0.5rem 0;
}

.formula-box {
  background: var(--gray-100);
  padding: 0.75rem;
  border-radius: 6px;
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  margin: 0.5rem 0;
  border-left: 3px solid var(--primary);
}

.formula-intro {
  background: var(--gray-100);
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
  color: var(--gray-700);
}

.calculation-table-wrapper {
  margin-top: 1rem;
  overflow-x: auto;
}

.calculation-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
  background: white;
}

.calculation-table th {
  background: var(--primary);
  color: white;
  padding: 0.75rem;
  text-align: left;
  font-weight: 600;
  position: sticky;
  top: 0;
}

.calculation-table td {
  padding: 0.75rem;
  border-bottom: 1px solid var(--gray-200);
}

.calculation-table tbody tr:hover {
  background: var(--gray-50);
}

.formula-cell {
  font-family: 'Courier New', monospace;
  font-size: 0.8rem;
  color: var(--gray-600);
  max-width: 300px;
  word-break: break-all;
}

.table-note,
.table-intro {
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: var(--gray-500);
  font-style: italic;
}

.table-intro {
  margin-bottom: 0.5rem;
  font-style: normal;
  font-weight: 500;
}

.table-controls {
  margin-bottom: 0.75rem;
  display: flex;
  justify-content: flex-end;
}

.btn-toggle-table {
  padding: 0.5rem 1rem;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-toggle-table:hover {
  background: var(--primary-dark);
}

.best-alpha {
  background: #d1fae5 !important;
}

.best-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: var(--primary);
  color: white;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
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

