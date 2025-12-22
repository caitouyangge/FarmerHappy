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
           <div class="model-selector">
             <label class="radio-option">
               <input 
                 type="radio" 
                 v-model="modelType" 
                 value="timeseries"
                 class="radio-input"
               />
               <span class="radio-label">ARIMA模型</span>
             </label>
             <label class="radio-option">
               <input 
                 type="radio" 
                 v-model="modelType" 
                 value="ai"
                 class="radio-input"
               />
               <span class="radio-label">AI预测</span>
             </label>
           </div>
           <div class="model-info-box" v-if="modelType === 'timeseries'">
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
           <div class="model-info-box" v-if="modelType === 'ai'">
            <div class="model-badge">AI预测模型</div>
             <p class="model-description">
              系统使用<strong>AI商品价格预测专家</strong>进行价格预测。AI模型基于深度学习和大数据分析，
              能够综合分析历史价格数据、市场趋势、季节性因素等多种信息，提供智能化的价格预测。
              AI模型能够识别复杂的价格模式和非线性关系，对于波动较大的商品价格具有更好的适应性。
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

          <!-- AI预测详情 / 详细计算过程 -->
          <div class="calculation-card">
            <h3 class="calculation-title" @click="toggleCalculationDetails">
              <span>{{ modelType === 'ai' ? 'AI预测详情' : '详细计算过程' }}</span>
              <span class="toggle-icon">{{ showCalculationDetails ? '▼' : '▶' }}</span>
            </h3>
            
            <div v-if="showCalculationDetails" class="calculation-content">
              <div v-if="!predictionResult.calculation_details" class="calculation-info">
                <p style="color: var(--gray-500);">详情正在加载中...</p>
              </div>
              
              <!-- AI预测详情 -->
              <template v-else-if="modelType === 'ai' && predictionResult.calculation_details">
                <div class="calculation-section">
                  <h4 class="section-subtitle">AI预测信息</h4>
                  <div class="calculation-info">
                    <p><strong>模型名称：</strong>{{ predictionResult.calculation_details.model_name || 'AI预测模型' }}</p>
                    <p><strong>预测方法：</strong>{{ predictionResult.calculation_details.prediction_method || 'AI商品价格预测专家' }}</p>
                    
                    <!-- 历史价格特征分析 -->
                    <div v-if="predictionResult.calculation_details.historical_features" class="formula-box" style="margin-bottom: var(--spacing-4); background: #e8f4f8; border-left-color: #2196F3;">
                      <p style="font-size: var(--font-lg); margin-bottom: var(--spacing-3);"><strong>📊 历史价格特征深度分析</strong></p>
                      
                      <div style="margin-bottom: var(--spacing-3);">
                        <p><strong>【基础统计信息】</strong></p>
                        <p>价格范围：¥{{ predictionResult.calculation_details.historical_features.min_price }} ~ ¥{{ predictionResult.calculation_details.historical_features.max_price }}</p>
                        <p>平均价格：¥{{ predictionResult.calculation_details.historical_features.avg_price }}</p>
                        <p>中位数价格：¥{{ predictionResult.calculation_details.historical_features.median_price }}</p>
                        <p>价格波动幅度：¥{{ predictionResult.calculation_details.historical_features.price_range }}</p>
                        <p>变异系数（CV）：{{ predictionResult.calculation_details.historical_features.coefficient_of_variation }}（值越大表示波动越大）</p>
                      </div>
                      
                      <div style="margin-bottom: var(--spacing-3);">
                        <p><strong>【趋势分析】</strong></p>
                        <p>整体趋势：{{ predictionResult.calculation_details.historical_features.overall_trend }}</p>
                        <p>趋势强度：{{ predictionResult.calculation_details.historical_features.trend_strength }}（0-1之间，值越大趋势越明显）</p>
                        <p v-if="predictionResult.calculation_details.historical_features.recent_trend">
                          近期趋势（最近30%数据）：{{ predictionResult.calculation_details.historical_features.recent_trend }}
                        </p>
                      </div>
                      
                      <div style="margin-bottom: var(--spacing-3);">
                        <p><strong>【波动性分析】</strong></p>
                        <p>标准差：¥{{ predictionResult.calculation_details.historical_features.std_dev }}</p>
                        <p>波动性评级：{{ predictionResult.calculation_details.historical_features.volatility_level }}</p>
                        <p v-if="predictionResult.calculation_details.historical_features.peak_price">
                          历史最高价：¥{{ predictionResult.calculation_details.historical_features.peak_price }}（日期：{{ predictionResult.calculation_details.historical_features.peak_date }}）
                        </p>
                        <p v-if="predictionResult.calculation_details.historical_features.trough_price">
                          历史最低价：¥{{ predictionResult.calculation_details.historical_features.trough_price }}（日期：{{ predictionResult.calculation_details.historical_features.trough_date }}）
                        </p>
                      </div>
                      
                      <div v-if="predictionResult.calculation_details.historical_features.has_seasonality" style="margin-bottom: var(--spacing-3);">
                        <p><strong>【季节性特征】</strong></p>
                        <p>检测到季节性模式，周期长度：{{ predictionResult.calculation_details.historical_features.seasonal_period }}天</p>
                      </div>
                      
                      <div>
                        <p><strong>【价格分布特征】</strong></p>
                        <p>价格主要集中在：¥{{ predictionResult.calculation_details.historical_features.q25_price }} ~ ¥{{ predictionResult.calculation_details.historical_features.q75_price }}之间（四分位距）</p>
                      </div>
                    </div>
                    
                    <!-- 预测理由 -->
                    <div v-if="predictionResult.calculation_details.prediction_reason" class="formula-box" style="margin-bottom: var(--spacing-4); background: #f0f9ff; border-left-color: #4CAF50;">
                      <p style="font-size: var(--font-lg); margin-bottom: var(--spacing-3);"><strong>💡 AI预测理由与分析</strong></p>
                      <div style="white-space: pre-wrap; line-height: 1.8; color: var(--gray-700);">
                        {{ predictionResult.calculation_details.prediction_reason }}
                      </div>
                    </div>
                    
                    <div v-if="predictionResult.calculation_details.ai_info" class="formula-box">
                      <p><strong>历史数据信息：</strong></p>
                      <p>数据点数：{{ predictionResult.calculation_details.ai_info.historical_data_count }}</p>
                      <p>起始日期：{{ predictionResult.calculation_details.ai_info.first_date }}</p>
                      <p>结束日期：{{ predictionResult.calculation_details.ai_info.last_date }}</p>
                      <p>起始价格：¥{{ predictionResult.calculation_details.ai_info.first_price }}</p>
                      <p>结束价格：¥{{ predictionResult.calculation_details.ai_info.last_price }}</p>
                      <p><strong>预测信息：</strong></p>
                      <p>预测天数：{{ predictionResult.calculation_details.ai_info.prediction_days }}</p>
                      <p>解析的预测数据点数：{{ predictionResult.calculation_details.ai_info.parsed_predicted_count }}</p>
                    </div>
                    
                    <div v-if="predictionResult.predicted_data && predictionResult.predicted_data.length > 0" class="formula-box" style="margin-top: var(--spacing-4);">
                      <p><strong>预测数据预览（前10条）：</strong></p>
                      <table class="preview-table" style="width: 100%; margin-top: var(--spacing-2);">
                        <thead>
                          <tr>
                            <th>日期</th>
                            <th>预测价格</th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr v-for="(item, index) in predictionResult.predicted_data.slice(0, 10)" :key="index">
                            <td>{{ item.date }}</td>
                            <td>¥{{ item.price }}</td>
                          </tr>
                        </tbody>
                      </table>
                      <p v-if="predictionResult.predicted_data.length > 10" style="margin-top: var(--spacing-2); color: var(--gray-500);">
                        共{{ predictionResult.predicted_data.length }}条预测数据，仅显示前10条
                      </p>
                    </div>
                    
                  
                  </div>
                </div>
              </template>
              
              <!-- ARIMA模型详细计算过程 -->
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
import { ref, onMounted, nextTick, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { pricePredictionService } from '../api/pricePrediction';
import logger from '../utils/logger';
import * as echarts from 'echarts';

export default {
  name: 'PricePrediction',
  setup() {
    const router = useRouter();
    const fileInput = ref(null);
    const chartContainer = ref(null);
    let chartInstance = null;
    let resizeHandler = null;
    
    const step = ref(1);
    const isDragOver = ref(false);
    const uploadedFile = ref(null);
    const fileId = ref(null);
    const previewData = ref([]);
    const totalRecords = ref(0);
    const uploading = ref(false);
    const predicting = ref(false);
    const predictionDays = ref(30);
    const modelType = ref('timeseries'); // 支持timeseries和ai两种模型
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

    // 绘制图表（使用 ECharts）：支持按规格/类型绘制多条曲线，并支持交互式工具提示
    const drawChart = () => {
      if (!chartContainer.value || !predictionResult.value) return;

      // 销毁旧图表实例
      if (chartInstance) {
        chartInstance.dispose();
        chartInstance = null;
      }

      // 创建新的图表实例
      chartInstance = echarts.init(chartContainer.value);

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

      // 找到价格的最大值和最小值
      const prices = allPoints.map(d => d.price);
      const minPrice = Math.min(...prices);
      const maxPrice = Math.max(...prices);
      const priceRange = maxPrice - minPrice || 1;
      const pricePadding = priceRange * 0.1; // 上下留10%的边距

      // 计算"历史-预测分界线"：取所有序列历史最后日期的最大值
      let dividerDate = null;
      seriesList.forEach(s => {
        const hist = s.historical_data || [];
        if (hist.length === 0) return;
        const last = hist[hist.length - 1].date;
        if (!dividerDate || new Date(last) > new Date(dividerDate)) dividerDate = last;
      });

      // 颜色盘
      const palette = ['#4CAF50', '#2196F3', '#9C27B0', '#FF5722', '#009688', '#795548', '#607D8B', '#E91E63'];

      // 构建 ECharts 系列数据
      const series = [];
      const legendData = [];

      seriesList.forEach((s, idx) => {
        const color = palette[idx % palette.length];
        const typeName = s.type || '默认';
        legendData.push(typeName);

        const hist = (s.historical_data || []).slice().sort((a, b) => new Date(a.date) - new Date(b.date));
        const pred = (s.predicted_data || []).slice().sort((a, b) => new Date(a.date) - new Date(b.date));

        // 历史数据系列
        if (hist.length > 0) {
          // 将历史数据转换为按日期索引的数组
          const histData = dates.map(date => {
            const point = hist.find(p => p.date === date);
            return point ? point.price : null;
          });
          series.push({
            name: `${typeName}（历史）`,
            type: 'line',
            data: histData,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              color: color,
              width: 2.5
            },
            itemStyle: {
              color: color
            },
            emphasis: {
              focus: 'series',
              itemStyle: {
                color: color,
                borderColor: '#fff',
                borderWidth: 2,
                shadowBlur: 10,
                shadowColor: color
              }
            },
            markPoint: {
              data: [
                { type: 'max', name: '最大值' },
                { type: 'min', name: '最小值' }
              ],
              itemStyle: {
                color: color
              }
            },
            // 添加历史-预测分界线（只在第一个历史系列中添加）
            ...(idx === 0 && dividerDate && dates.includes(dividerDate) ? {
              markLine: {
                silent: true,
                lineStyle: {
                  color: '#999',
                  type: 'dashed',
                  width: 1.5
                },
                label: {
                  show: true,
                  position: 'insideEndTop',
                  formatter: '历史/预测分界',
                  color: '#999',
                  fontSize: 10
                },
                data: [{
                  xAxis: dividerDate
                }]
              }
            } : {})
          });
        }

        // 预测数据系列
        if (pred.length > 0) {
          // 获取历史数据的最后一个点（用于连接）
          const lastHistPoint = hist.length > 0 ? hist[hist.length - 1] : null;
          const lastHistDate = lastHistPoint ? lastHistPoint.date : null;
          
          // 将预测数据转换为按日期索引的数组
          const predData = dates.map(date => {
            // 如果是历史数据的最后一个日期，且预测数据中没有这个日期，使用历史数据的最后一个价格值来连接
            if (date === lastHistDate && !pred.find(p => p.date === date)) {
              return lastHistPoint.price;
            }
            const point = pred.find(p => p.date === date);
            return point ? point.price : null;
          });
          series.push({
            name: `${typeName}（预测）`,
            type: 'line',
            data: predData,
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: {
              color: color,
              width: 2.5,
              type: 'dashed'
            },
            itemStyle: {
              color: color
            },
            emphasis: {
              focus: 'series',
              itemStyle: {
                color: color,
                borderColor: '#fff',
                borderWidth: 2,
                shadowBlur: 10,
                shadowColor: color
              }
            }
          });
        }
      });

      // 配置选项
      const option = {
        backgroundColor: 'transparent',
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross',
            label: {
              backgroundColor: '#6a7985'
            },
            crossStyle: {
              color: '#999'
            }
          },
          formatter: function(params) {
            let result = `<div style="margin-bottom: 4px; font-weight: 600; color: #333;">${params[0].axisValue}</div>`;
            params.forEach(param => {
              if (param.value === null || param.value === undefined) return;
              const isPredicted = param.seriesName.includes('预测');
              const typeName = param.seriesName.replace('（历史）', '').replace('（预测）', '');
              const price = Array.isArray(param.value) ? param.value[1] : param.value;
              result += `
                <div style="margin: 4px 0; display: flex; align-items: center;">
                  <span style="display: inline-block; width: 10px; height: 10px; background: ${param.color}; border-radius: 50%; margin-right: 8px;"></span>
                  <span style="color: #666;">${typeName}${isPredicted ? '（预测）' : '（历史）'}:</span>
                  <span style="margin-left: 8px; font-weight: 600; color: ${param.color};">¥${price.toFixed(2)}</span>
                </div>
              `;
            });
            return result;
          },
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#e0e0e0',
          borderWidth: 1,
          padding: [10, 15],
          textStyle: {
            fontSize: 12,
            color: '#333'
          },
          extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15); border-radius: 8px;'
        },
        legend: {
          data: legendData,
          top: 10,
          right: 20,
          textStyle: {
            fontSize: 12,
            color: '#666'
          },
          itemGap: 20,
          itemWidth: 14,
          itemHeight: 14
        },
        grid: {
          left: '10%',
          right: '10%',
          top: '15%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLine: {
            lineStyle: {
              color: '#ccc'
            }
          },
          axisLabel: {
            color: '#666',
            fontSize: 11,
            rotate: 45,
            formatter: function(value) {
              // 如果日期字符串格式为 YYYY-MM-DD，只显示月/日
              if (value && value.length >= 10) {
                return value.substring(5, 10).replace('-', '/');
              }
              return value;
            }
          },
          splitLine: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          name: '价格（元）',
          nameLocation: 'middle',
          nameGap: 50,
          nameTextStyle: {
            color: '#666',
            fontSize: 12
          },
          min: minPrice - pricePadding,
          max: maxPrice + pricePadding,
          axisLine: {
            lineStyle: {
              color: '#ccc'
            }
          },
          axisLabel: {
            color: '#666',
            fontSize: 11,
            formatter: function(value) {
              return '¥' + value.toFixed(2);
            }
          },
          splitLine: {
            lineStyle: {
              color: '#f0f0f0',
              type: 'dashed'
            }
          }
        },
        dataZoom: [
          {
            type: 'inside',
            start: 0,
            end: 100
          },
          {
            type: 'slider',
            start: 0,
            end: 100,
            height: 20,
            bottom: 10,
            handleStyle: {
              color: '#6b73ff'
            },
            dataBackground: {
              areaStyle: {
                color: 'rgba(107, 115, 255, 0.1)'
              }
            },
            selectedDataBackground: {
              areaStyle: {
                color: 'rgba(107, 115, 255, 0.2)'
              }
            }
          }
        ],
        series: series
      };

      // 设置配置并渲染
      chartInstance.setOption(option);

      // 响应式调整
      resizeHandler = () => {
        if (chartInstance) {
          chartInstance.resize();
        }
      };
      window.addEventListener('resize', resizeHandler);
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
      // 清理图表实例
      if (chartInstance) {
        chartInstance.dispose();
        chartInstance = null;
      }
    };

    // 组件卸载时清理资源
    onBeforeUnmount(() => {
      if (chartInstance) {
        chartInstance.dispose();
        chartInstance = null;
      }
      // 移除窗口resize事件监听器
      if (resizeHandler) {
        window.removeEventListener('resize', resizeHandler);
        resizeHandler = null;
      }
    });

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
  padding: var(--spacing-4) var(--spacing-8);
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  box-shadow: var(--shadow-card);
}

.btn-back {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-2) var(--spacing-4);
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: var(--radius-md);
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
  font-size: var(--font-2xl);
  font-weight: var(--font-semibold);
  color: var(--primary);
  margin: 0;
}

.main-content {
  padding: var(--spacing-8);
  max-width: 1200px;
  margin: 0 auto;
}

.step-section {
  background: var(--white);
  padding: var(--spacing-8);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
}

.section-title {
  font-size: var(--font-2xl);
  font-weight: var(--font-semibold);
  color: var(--gray-900);
  margin-bottom: var(--spacing-8);
}

.upload-area {
  border: 2px dashed var(--gray-300);
  border-radius: var(--radius-lg);
  padding: var(--spacing-12);
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
  font-size: var(--font-4xl);
  margin-bottom: var(--spacing-4);
}

.upload-text {
  font-size: var(--font-lg);
  font-weight: var(--font-medium);
  color: var(--gray-900);
  margin-bottom: var(--spacing-2);
}

.upload-hint {
  font-size: var(--font-sm);
  color: var(--gray-500);
  margin-bottom: var(--spacing-8);
}

.format-example {
  margin-top: var(--spacing-8);
  text-align: left;
  display: inline-block;
}

.example-table {
  border-collapse: collapse;
  margin-top: var(--spacing-2);
}

.example-table th,
.example-table td {
  border: 1px solid var(--gray-300);
  padding: var(--spacing-2) var(--spacing-4);
  text-align: left;
}

.example-table th {
  background: var(--gray-100);
  font-weight: var(--font-semibold);
}

.file-info {
  margin-top: var(--spacing-6);
}

.file-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-4);
  padding: var(--spacing-4);
  background: var(--gray-50);
  border-radius: var(--radius-md);
}

.file-name {
  font-weight: var(--font-medium);
  color: var(--gray-900);
}

.file-size {
  color: var(--gray-500);
  font-size: var(--font-sm);
}

.btn-remove {
  margin-left: auto;
  padding: var(--spacing-1) var(--spacing-3);
  background: var(--error);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-sm);
}

.preview-section {
  margin-top: var(--spacing-8);
}

.preview-title {
  font-size: var(--font-lg);
  font-weight: var(--font-semibold);
  margin-bottom: var(--spacing-4);
}

.preview-table-wrapper {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--gray-300);
  border-radius: var(--radius-md);
}

.preview-table {
  width: 100%;
  border-collapse: collapse;
}

.preview-table th,
.preview-table td {
  padding: var(--spacing-3);
  text-align: left;
  border-bottom: 1px solid var(--gray-200);
}

.preview-table th {
  background: var(--gray-100);
  font-weight: var(--font-semibold);
  position: sticky;
  top: 0;
}

.preview-total {
  margin-top: var(--spacing-2);
  color: var(--gray-600);
  font-size: var(--font-sm);
  line-height: var(--leading-relaxed);
}

.preview-note {
  display: block;
  margin-top: var(--spacing-1);
  color: var(--primary);
  font-size: var(--font-sm);
  font-weight: var(--font-medium);
}

.form-group {
  margin-bottom: var(--spacing-6);
}

.form-label {
  display: block;
  font-weight: var(--font-medium);
  margin-bottom: var(--spacing-2);
  color: var(--gray-900);
}

.form-input,
.form-select {
  width: 100%;
  padding: var(--spacing-3);
  border: 1px solid var(--gray-300);
  border-radius: var(--radius-md);
  font-size: var(--font-base);
}

 .form-hint {
   margin-top: var(--spacing-1);
   font-size: var(--font-sm);
   color: var(--gray-500);
 }

 .model-info-box {
   padding: var(--spacing-4);
   background: var(--gray-50);
   border-radius: var(--radius-md);
   border: 1px solid var(--gray-200);
 }

 .model-badge {
   display: inline-block;
   padding: var(--spacing-2) var(--spacing-4);
   background: var(--primary);
   color: white;
   border-radius: var(--radius-sm);
   font-weight: var(--font-medium);
   margin-bottom: var(--spacing-3);
 }

 .model-description {
   font-size: var(--font-sm);
   color: var(--gray-600);
   line-height: var(--leading-relaxed);
   margin: 0;
 }

 .model-selector {
   display: flex;
   gap: var(--spacing-4);
   margin-bottom: var(--spacing-4);
 }

 .radio-option {
   display: flex;
   align-items: center;
   gap: var(--spacing-2);
   padding: var(--spacing-3) var(--spacing-4);
   border: 2px solid var(--gray-300);
   border-radius: var(--radius-md);
   cursor: pointer;
   transition: all 0.2s;
   background: var(--white);
 }

 .radio-option:hover {
   border-color: var(--primary);
   background: var(--primary-light);
 }

 .radio-option input[type="radio"]:checked + .radio-label {
   color: var(--primary);
   font-weight: var(--font-semibold);
 }

 .radio-option:has(input[type="radio"]:checked) {
   border-color: var(--primary);
   background: var(--primary-light);
 }

 .radio-input {
   margin: 0;
   cursor: pointer;
 }

 .radio-label {
   font-size: var(--font-base);
   color: var(--gray-700);
   cursor: pointer;
   user-select: none;
 }

.action-buttons {
  display: flex;
  gap: var(--spacing-4);
  margin-top: var(--spacing-8);
}

.btn-primary {
  flex: 1;
  padding: var(--spacing-3) var(--spacing-6);
  background: var(--primary);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-base);
  font-weight: var(--font-medium);
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
  padding: var(--spacing-3) var(--spacing-6);
  background: var(--gray-200);
  color: var(--gray-900);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-base);
  font-weight: var(--font-medium);
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
  padding: var(--spacing-6);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-6);
  box-shadow: var(--shadow-card);
}

.metrics-title,
.trend-title,
.chart-title,
.data-title {
  font-size: var(--font-lg);
  font-weight: var(--font-semibold);
  margin-bottom: var(--spacing-4);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--spacing-4);
}

.metric-item {
  text-align: center;
  padding: var(--spacing-4);
  background: white;
  border-radius: var(--radius-md);
}

.metric-label {
  font-size: var(--font-sm);
  color: var(--gray-500);
  margin-bottom: var(--spacing-2);
}

.metric-value {
  font-size: var(--font-2xl);
  font-weight: var(--font-semibold);
  color: var(--primary);
  margin-bottom: var(--spacing-1);
}

.metric-desc {
  font-size: var(--font-xs);
  color: var(--gray-400);
}

.trend-badge {
  display: inline-block;
  padding: var(--spacing-3) var(--spacing-6);
  border-radius: var(--radius-md);
  font-size: var(--font-lg);
  font-weight: var(--font-medium);
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
  height: 500px;
  background: white;
  border-radius: var(--radius-md);
  padding: var(--spacing-4);
  box-shadow: var(--shadow-sm);
}

.data-table-wrapper {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid var(--gray-300);
  border-radius: var(--radius-md);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: var(--spacing-3);
  text-align: left;
  border-bottom: 1px solid var(--gray-200);
}

.data-table th {
  background: var(--gray-100);
  font-weight: var(--font-semibold);
  position: sticky;
  top: 0;
}

.error-message {
  margin-top: var(--spacing-4);
  padding: var(--spacing-4);
  background: var(--error-light);
  color: var(--error-dark);
  border-radius: var(--radius-md);
  border: 1px solid var(--error-light);
}

.calculation-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  padding: var(--spacing-2) 0;
  font-size: var(--font-lg);
  font-weight: var(--font-semibold);
  color: var(--primary);
  transition: color 0.2s;
}

.calculation-title:hover {
  color: var(--primary-dark);
}

.toggle-icon {
  font-size: var(--font-sm);
  transition: transform 0.2s;
}

.calculation-content {
  margin-top: var(--spacing-4);
  padding-top: var(--spacing-4);
  border-top: 1px solid var(--gray-300);
}

.calculation-section {
  margin-bottom: var(--spacing-8);
  padding: var(--spacing-4);
  background: white;
  border-radius: var(--radius-md);
  border-left: var(--spacing-1) solid var(--primary);
}

.section-subtitle {
  font-size: var(--font-base);
  font-weight: var(--font-semibold);
  color: var(--primary);
  margin-bottom: var(--spacing-3);
}

.calculation-info {
  font-size: var(--font-sm);
  line-height: var(--leading-relaxed);
  color: var(--gray-700);
}

.calculation-info p {
  margin: var(--spacing-2) 0;
}

.formula-box {
  background: var(--gray-100);
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
  font-family: 'Courier New', monospace;
  font-size: var(--font-sm);
  margin: var(--spacing-2) 0;
  border-left: 3px solid var(--primary);
}

.formula-intro {
  background: var(--gray-100);
  padding: var(--spacing-3);
  border-radius: var(--radius-sm);
  font-size: var(--font-sm);
  margin-bottom: var(--spacing-3);
  color: var(--gray-700);
}

.calculation-table-wrapper {
  margin-top: var(--spacing-4);
  overflow-x: auto;
}

.calculation-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-sm);
  background: white;
}

.calculation-table th {
  background: var(--primary);
  color: white;
  padding: var(--spacing-3);
  text-align: left;
  font-weight: var(--font-semibold);
  position: sticky;
  top: 0;
}

.calculation-table td {
  padding: var(--spacing-3);
  border-bottom: 1px solid var(--gray-200);
}

.calculation-table tbody tr:hover {
  background: var(--gray-50);
}

.formula-cell {
  font-family: 'Courier New', monospace;
  font-size: var(--font-xs);
  color: var(--gray-600);
  max-width: 300px;
  word-break: break-all;
}

.table-note,
.table-intro {
  margin-top: var(--spacing-2);
  font-size: var(--font-xs);
  color: var(--gray-500);
  font-style: italic;
}

.table-intro {
  margin-bottom: var(--spacing-2);
  font-style: normal;
  font-weight: var(--font-medium);
}

.table-controls {
  margin-bottom: var(--spacing-3);
  display: flex;
  justify-content: flex-end;
}

.btn-toggle-table {
  padding: var(--spacing-2) var(--spacing-4);
  background: var(--primary);
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-sm);
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

@media (max-width: 1200px) {
  .metrics-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
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

