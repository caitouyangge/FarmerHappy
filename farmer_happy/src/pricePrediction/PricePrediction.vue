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
             <div class="model-badge">ARIMA模型（自回归综合移动平均）</div>
             <p class="model-description">
               系统使用ARIMA（AutoRegressive Integrated Moving Average）模型进行预测。
               ARIMA模型是经典的时间序列预测模型，特别适用于具有明显线性趋势和季节性的农产品价格。
               模型会自动选择最优参数（p, d, q），并支持季节性ARIMA（SARIMA）以捕捉周期性规律。
               该模型简单、可解释性强，是时间序列预测的基准模型。
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
                  <p v-if="predictionResult.calculation_details.preprocessing.removed_count > 0">
                    <strong>去除异常值数量：</strong>{{ predictionResult.calculation_details.preprocessing.removed_count }}
                  </p>
                  <p v-if="predictionResult.calculation_details.preprocessing.mean">
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

              <!-- ARIMA模型参数 -->
              <div v-if="predictionResult.calculation_details.arima_params" class="calculation-section">
                <h4 class="section-subtitle">2. ARIMA模型参数选择</h4>
                <div class="calculation-info">
                  <p class="formula-intro">
                    <strong>ARIMA模型说明：</strong><br>
                    • <strong>ARIMA(p,d,q)</strong>：p=自回归阶数，d=差分次数，q=移动平均阶数<br>
                    • <strong>SARIMA(p,d,q)(P,D,Q,s)</strong>：包含季节性参数，s=季节周期<br>
                    • <strong>p (AR)</strong>：使用过去p个时间点的值来预测当前值<br>
                    • <strong>d (差分)</strong>：对数据进行d次差分以消除趋势，使序列平稳<br>
                    • <strong>q (MA)</strong>：使用过去q个时间点的预测误差来改进预测<br>
                    <strong>选择方法：</strong>{{ predictionResult.calculation_details.arima_params.method }}
                  </p>
                  <p><strong>选择的模型类型：</strong>{{ predictionResult.calculation_details.arima_params.model_type }}</p>
                  <div class="formula-box">
                    <p><strong>模型参数：</strong></p>
                    <p>• p (AR阶数) = {{ predictionResult.calculation_details.arima_params.p }}</p>
                    <p>• d (差分次数) = {{ predictionResult.calculation_details.arima_params.d }}</p>
                    <p>• q (MA阶数) = {{ predictionResult.calculation_details.arima_params.q }}</p>
                    <div v-if="predictionResult.calculation_details.arima_params.is_seasonal">
                      <p><strong>季节性参数：</strong></p>
                      <p>• P (季节性AR) = {{ predictionResult.calculation_details.arima_params.P }}</p>
                      <p>• D (季节性差分) = {{ predictionResult.calculation_details.arima_params.D }}</p>
                      <p>• Q (季节性MA) = {{ predictionResult.calculation_details.arima_params.Q }}</p>
                      <p>• s (季节周期) = {{ predictionResult.calculation_details.arima_params.s }}天</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ARIMA模型计算 -->
              <div v-if="predictionResult.calculation_details.model_calculation" class="calculation-section">
                <h4 class="section-subtitle">3. ARIMA模型计算过程</h4>
                <p class="formula-intro">
                  <strong>ARIMA模型公式：</strong>(1-φ₁B-φ₂B²-...-φₚBᵖ)(1-B)ᵈY(t) = (1+θ₁B+θ₂B²+...+θₑBᵉ)ε(t)<br>
                  <strong>其中：</strong>B是滞后算子，φ是AR系数，θ是MA系数，ε(t)是白噪声误差项<br>
                  <strong>简化形式：</strong>Y(t) = c + φ₁Y(t-1) + ... + φₚY(t-p) + ε(t) + θ₁ε(t-1) + ... + θₑε(t-q)
                </p>
                
                <!-- AR系数 -->
                <div v-if="predictionResult.calculation_details.model_calculation.ar_coefficients" class="calculation-info">
                  <h5 style="margin-top: 1rem; margin-bottom: 0.5rem; color: var(--primary);">AR（自回归）系数：</h5>
                  <div class="calculation-table-wrapper">
                    <table class="calculation-table">
                      <thead>
                        <tr>
                          <th>阶数</th>
                          <th>系数符号</th>
                          <th>系数值</th>
                          <th>说明</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(coeff, index) in predictionResult.calculation_details.model_calculation.ar_coefficients" :key="index">
                          <td>{{ coeff.order }}</td>
                          <td>φ{{ coeff.order }}</td>
                          <td>{{ coeff.value }}</td>
                          <td>{{ coeff.description }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
                
                <!-- MA系数 -->
                <div v-if="predictionResult.calculation_details.model_calculation.ma_coefficients" class="calculation-info">
                  <h5 style="margin-top: 1rem; margin-bottom: 0.5rem; color: var(--primary);">MA（移动平均）系数：</h5>
                  <div class="calculation-table-wrapper">
                    <table class="calculation-table">
                      <thead>
                        <tr>
                          <th>阶数</th>
                          <th>系数符号</th>
                          <th>系数值</th>
                          <th>说明</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(coeff, index) in predictionResult.calculation_details.model_calculation.ma_coefficients" :key="index">
                          <td>{{ coeff.order }}</td>
                          <td>θ{{ coeff.order }}</td>
                          <td>{{ coeff.value }}</td>
                          <td>{{ coeff.description }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
                
                <!-- 差分过程 -->
                <div v-if="predictionResult.calculation_details.model_calculation.differencing_steps" class="calculation-info">
                  <h5 style="margin-top: 1rem; margin-bottom: 0.5rem; color: var(--primary);">差分过程：</h5>
                  <div v-for="(step, index) in predictionResult.calculation_details.model_calculation.differencing_steps" :key="index" class="formula-box">
                    <p><strong>步骤{{ step.step }}：</strong>{{ step.description }}</p>
                    <p>原始数据点数：{{ step.original_count }}，差分后数据点数：{{ step.differenced_count }}</p>
                  </div>
                </div>
                
                <!-- 残差信息 -->
                <div v-if="predictionResult.calculation_details.model_calculation.residual_info" class="calculation-info">
                  <h5 style="margin-top: 1rem; margin-bottom: 0.5rem; color: var(--primary);">残差统计：</h5>
                  <div class="formula-box">
                    <p>残差数量：{{ predictionResult.calculation_details.model_calculation.residual_info.count }}</p>
                    <p>残差均值：{{ predictionResult.calculation_details.model_calculation.residual_info.mean }}</p>
                    <p>残差标准差：{{ predictionResult.calculation_details.model_calculation.residual_info.std }}</p>
                    <p><strong>说明：</strong>残差应该接近白噪声（均值为0，无自相关），这是模型拟合良好的标志</p>
                  </div>
                </div>
              </div>

              <!-- 评估指标计算 -->
              <div v-if="predictionResult.calculation_details.model_calculation && predictionResult.calculation_details.model_calculation.evaluation_calculation" class="calculation-section">
                <h4 class="section-subtitle">4. 模型评估指标计算</h4>

              <!-- 预测过程 -->
              <div v-if="predictionResult.calculation_details.prediction_steps" class="calculation-section">
                <h4 class="section-subtitle">5. 预测计算过程</h4>
                <div class="calculation-info">
                  <p class="formula-intro">
                    <strong>评估指标说明：</strong><br>
                    • <strong>R²（决定系数）</strong>：衡量模型对数据变异的解释程度，范围0-1，越接近1越好<br>
                    • <strong>MAE（平均绝对误差）</strong>：预测值与实际值差的绝对值的平均值，越小越好<br>
                    • <strong>RMSE（均方根误差）</strong>：预测误差的平方根，越小越好
                  </p>
                  <p><strong>实际价格均值（ȳ）：</strong>{{ predictionResult.calculation_details.model_calculation.evaluation_calculation.y_mean }}</p>
                  <p class="formula-box">
                    <strong>R²计算公式：</strong>R² = 1 - (SS_res / SS_tot)<br>
                    {{ predictionResult.calculation_details.model_calculation.evaluation_calculation.r_squared_formula }}
                  </p>
                  <p class="formula-box">
                    <strong>MAE计算公式：</strong>MAE = (1/n) × Σ|实际值 - 预测值|<br>
                    {{ predictionResult.calculation_details.model_calculation.evaluation_calculation.mae_formula }}
                  </p>
                  <p class="formula-box">
                    <strong>RMSE计算公式：</strong>RMSE = √[(1/n) × Σ(实际值 - 预测值)²]<br>
                    {{ predictionResult.calculation_details.model_calculation.evaluation_calculation.rmse_formula }}
                  </p>
                  <div v-if="predictionResult.calculation_details.model_calculation.evaluation_calculation.steps" class="calculation-table-wrapper">
                    <div class="table-controls">
                      <button 
                        v-if="predictionResult.calculation_details.model_calculation.evaluation_calculation.steps.length > 20"
                        @click="showAllEvaluationSteps = !showAllEvaluationSteps"
                        class="btn-toggle-table">
                        {{ showAllEvaluationSteps ? '收起' : '展开全部' }}（共{{ predictionResult.calculation_details.model_calculation.evaluation_calculation.steps.length }}条）
                      </button>
                    </div>
                    <p class="table-intro">详细计算步骤：</p>
                    <table class="calculation-table">
                      <thead>
                        <tr>
                          <th>序号</th>
                          <th>实际值</th>
                          <th>预测值</th>
                          <th>误差 (实际-预测)</th>
                          <th>误差²</th>
                          <th>(实际-均值)²</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(step, index) in (showAllEvaluationSteps ? predictionResult.calculation_details.model_calculation.evaluation_calculation.steps : predictionResult.calculation_details.model_calculation.evaluation_calculation.steps.slice(0, 20))" :key="index">
                          <td>{{ step.index }}</td>
                          <td>{{ step.actual }}</td>
                          <td>{{ step.predicted }}</td>
                          <td>{{ step.error }}</td>
                          <td>{{ step.error_squared }}</td>
                          <td>{{ step.total_squared }}</td>
                        </tr>
                      </tbody>
                    </table>
                    <p v-if="!showAllEvaluationSteps && predictionResult.calculation_details.model_calculation.evaluation_calculation.steps.length > 20" class="table-note">
                      显示前20条，共{{ predictionResult.calculation_details.model_calculation.evaluation_calculation.steps.length }}条数据，点击"展开全部"查看完整数据
                    </p>
                    <p class="table-note">
                      <strong>汇总结果：</strong><br>
                      残差平方和(SS_res) = Σ(误差²) = {{ predictionResult.calculation_details.model_calculation.evaluation_calculation.ss_res }}<br>
                      总平方和(SS_tot) = Σ(实际值-均值)² = {{ predictionResult.calculation_details.model_calculation.evaluation_calculation.ss_tot }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- 预测过程（已移到上面） -->
                <p class="formula-intro">
                  <strong>预测公式：</strong>预测价格 = 最后平滑值 + 趋势 × 预测步数<br>
                  <strong>其中：</strong>最后平滑值 = 历史数据最后一个时间点的指数平滑值，趋势 = 历史数据的平均变化率，预测步数 = 从最后数据点到预测点的天数差<br>
                  <strong>说明：</strong>基于历史数据的趋势外推，假设未来价格变化遵循历史趋势
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
                        <th>预测步数</th>
                        <th>最后平滑值</th>
                        <th>趋势</th>
                        <th>计算公式</th>
                        <th>预测价格</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(step, index) in (showAllPredictionSteps ? predictionResult.calculation_details.prediction_steps : predictionResult.calculation_details.prediction_steps.slice(0, 20))" :key="index">
                        <td>{{ step.date }}</td>
                        <td>{{ step.steps }}</td>
                        <td>{{ step.last_smoothed }}</td>
                        <td>{{ step.trend }}</td>
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
      ctx.lineWidth = 2.5;
      ctx.beginPath();
      const historicalData = predictionResult.value.historical_data;
      const totalPoints = historicalData.length + predictionResult.value.predicted_data.length;
      historicalData.forEach((point, index) => {
        const x = padding + (index / (totalPoints - 1)) * chartWidth;
        const y = canvas.height - padding - ((point.price - minPrice) / priceRange) * chartHeight;
        if (index === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.stroke();
      
      // 绘制历史数据点
      ctx.fillStyle = '#4CAF50';
      historicalData.forEach((point, index) => {
        const x = padding + (index / (totalPoints - 1)) * chartWidth;
        const y = canvas.height - padding - ((point.price - minPrice) / priceRange) * chartHeight;
        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fill();
      });

      // 绘制预测数据（从历史数据最后一个点连续绘制）
      ctx.strokeStyle = '#FF9800';
      ctx.lineWidth = 2.5;
      ctx.setLineDash([8, 4]);
      ctx.beginPath();
      const predictedData = predictionResult.value.predicted_data;
      
      // 从历史数据的最后一个点开始
      if (historicalData.length > 0) {
        const lastHistoricalIndex = historicalData.length - 1;
        const lastX = padding + (lastHistoricalIndex / (totalPoints - 1)) * chartWidth;
        const lastY = canvas.height - padding - ((historicalData[lastHistoricalIndex].price - minPrice) / priceRange) * chartHeight;
        ctx.moveTo(lastX, lastY);
      }
      
      predictedData.forEach((point, index) => {
        const dataIndex = historicalData.length + index;
        const x = padding + (dataIndex / (totalPoints - 1)) * chartWidth;
        const y = canvas.height - padding - ((point.price - minPrice) / priceRange) * chartHeight;
        ctx.lineTo(x, y);
      });
      ctx.stroke();
      ctx.setLineDash([]);

      // 绘制预测数据点
      ctx.fillStyle = '#FF9800';
      predictedData.forEach((point, index) => {
        const dataIndex = historicalData.length + index;
        const x = padding + (dataIndex / (totalPoints - 1)) * chartWidth;
        const y = canvas.height - padding - ((point.price - minPrice) / priceRange) * chartHeight;
        ctx.beginPath();
        ctx.arc(x, y, 3, 0, Math.PI * 2);
        ctx.fill();
      });

      // 绘制分隔线（历史与预测的分界）
      const dividerX = padding + ((historicalData.length - 1) / (totalPoints - 1)) * chartWidth;
      ctx.strokeStyle = '#999';
      ctx.lineWidth = 1.5;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      ctx.moveTo(dividerX, padding);
      ctx.lineTo(dividerX, canvas.height - padding);
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

