<template>
  <div class="price-data-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <button class="btn-back" @click="goBack">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1 class="page-title">农产品价格数据获取</h1>
    </header>

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 步骤1: 输入查询条件 -->
      <div v-if="step === 1" class="step-section">
        <h2 class="section-title">步骤1: 输入查询条件</h2>
        
        <div class="form-group">
          <label class="form-label">开始时间</label>
          <input 
            type="date" 
            v-model="startTime" 
            class="form-input"
            :max="endTime || maxDate"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">结束时间</label>
          <input 
            type="date" 
            v-model="endTime" 
            class="form-input"
            :min="startTime"
            :max="maxDate"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label">产品名称</label>
          <input 
            type="text" 
            v-model="productName" 
            class="form-input"
            placeholder="例如：苹果、猪、大米等"
            required
          />
        </div>

        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>

        <div class="action-buttons">
          <button 
            class="btn-primary" 
            :disabled="!canQuery || loading"
            @click="fetchPriceData">
            {{ loading ? '获取中...' : '获取数据' }}
          </button>
        </div>
      </div>

      <!-- 步骤2: 选择要导出的产品 -->
      <div v-if="step === 2" class="step-section">
        <h2 class="section-title">步骤2: 选择要导出的产品</h2>
        <p class="section-desc">根据查询条件，共找到 {{ productList.length }} 种不同的产品（将导出为 split 文件夹，优先保存为 .xlsx）</p>

        <div class="product-selection">
          <div class="selection-header">
            <label class="checkbox-label">
              <input 
                type="checkbox" 
                v-model="selectAll"
                @change="handleSelectAll"
              />
              <span>全选</span>
            </label>
            <span class="selected-count">已选择 {{ selectedProducts.length }} 种产品</span>
          </div>

          <div class="product-list">
            <label 
              v-for="product in productList" 
              :key="product"
              class="product-item"
            >
              <input 
                type="checkbox" 
                :value="product"
                v-model="selectedProducts"
              />
              <span>{{ product }}</span>
            </label>
          </div>
        </div>

        <div class="action-buttons">
          <button class="btn-secondary" @click="step = 1">上一步</button>
          <button 
            class="btn-primary" 
            :disabled="selectedProducts.length === 0 || generating"
            @click="exportSplitFolder">
            {{ generating ? '保存中...' : '保存split文件夹（xlsx）' }}
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { priceDataService } from '../api/priceData';
import logger from '../utils/logger';
import Papa from 'papaparse';

export default {
  name: 'PriceData',
  setup() {
    const router = useRouter();
    const step = ref(1);
    const startTime = ref('');
    const endTime = ref('');
    const productName = ref('');
    const loading = ref(false);
    const generating = ref(false);
    const errorMessage = ref('');
    const csvData = ref([]);
    const productList = ref([]);
    const selectedProducts = ref([]);
    const selectAll = ref(false);
    const csvFileName = ref('');
    const splitFiles = ref([]); // 后端拆分后的文件列表（含 variety/file_name 等）

    // 最大日期为今天
    const maxDate = computed(() => {
      const today = new Date();
      return today.toISOString().split('T')[0];
    });

    // 是否可以查询
    const canQuery = computed(() => {
      return startTime.value && endTime.value && productName.value.trim();
    });

    // 返回上一页
    const goBack = () => {
      router.push('/home');
    };

    // 获取价格数据
    const fetchPriceData = async () => {
      if (!canQuery.value) {
        errorMessage.value = '请填写完整的查询条件';
        return;
      }

      // 验证日期
      if (new Date(startTime.value) > new Date(endTime.value)) {
        errorMessage.value = '开始时间不能晚于结束时间';
        return;
      }

      loading.value = true;
      errorMessage.value = '';

      try {
        logger.info('PRICE_DATA', '开始获取价格数据', {
          startTime: startTime.value,
          endTime: endTime.value,
          productName: productName.value
        });

        // 调用API获取数据
        const result = await priceDataService.getPriceData(
          startTime.value,
          endTime.value,
          productName.value
        );

        csvFileName.value = result.file_name;
        logger.info('PRICE_DATA', '获取数据成功', { fileName: csvFileName.value });

        // 优先使用后端返回的 split_files 构建可选品种列表（不再依赖总数据文件）
        if (Array.isArray(result.split_files) && result.split_files.length > 0) {
          splitFiles.value = result.split_files;
          const products = new Set();
          splitFiles.value.forEach(f => {
            if (f && f.variety) products.add(f.variety);
          });
          productList.value = Array.from(products).sort();
          selectedProducts.value = [];
          selectAll.value = false;
          step.value = 2;
        } else {
          // 兼容旧返回：下载并解析总 CSV，提取品名列表
          await downloadAndParseCsv(csvFileName.value);
        }

      } catch (error) {
        errorMessage.value = error.message || '获取数据失败，请稍后重试';
        logger.error('PRICE_DATA', '获取价格数据失败', {}, error);
      } finally {
        loading.value = false;
      }
    };

    // 下载并解析CSV文件
    const downloadAndParseCsv = async (fileName) => {
      try {
        logger.info('PRICE_DATA', '开始下载CSV文件', { fileName });
        
        const blob = await priceDataService.downloadCsvFile(fileName);
        
        // 将blob转换为文本
        const text = await blob.text();
        
        // 使用PapaParse解析CSV
        Papa.parse(text, {
          header: true,
          skipEmptyLines: true,
          complete: (results) => {
            csvData.value = results.data;
            
            // 提取所有不同的产品名称
            const products = new Set();
            csvData.value.forEach(row => {
              if (row['品名']) {
                products.add(row['品名']);
              }
            });
            
            productList.value = Array.from(products).sort();
            splitFiles.value = []; // 此模式下没有 split_files 元信息
            selectedProducts.value = [];
            selectAll.value = false;
            
            logger.info('PRICE_DATA', 'CSV解析完成', {
              totalRows: csvData.value.length,
              productCount: productList.value.length
            });

            // 进入步骤2
            step.value = 2;
          },
          error: (error) => {
            logger.error('PRICE_DATA', 'CSV解析失败', {}, error);
            errorMessage.value = 'CSV文件解析失败，请稍后重试';
          }
        });
      } catch (error) {
        logger.error('PRICE_DATA', '下载CSV文件失败', {}, error);
        errorMessage.value = error.message || '下载CSV文件失败，请稍后重试';
      }
    };

    // 全选/取消全选
    const handleSelectAll = () => {
      if (selectAll.value) {
        selectedProducts.value = [...productList.value];
      } else {
        selectedProducts.value = [];
      }
    };

    // 监听选中产品变化，自动更新全选状态
    watch(selectedProducts, () => {
      selectAll.value = selectedProducts.value.length === productList.value.length && productList.value.length > 0;
    }, { deep: true });

    // 保存 split 文件夹（优先保存选中的拆分 XLSX；若不存在则回退 CSV）
    const exportSplitFolder = async () => {
      if (selectedProducts.value.length === 0) {
        errorMessage.value = '请至少选择一个产品';
        return;
      }

      generating.value = true;
      errorMessage.value = '';

      try {
        // 必须有 splitFiles 元信息才能按“拆分文件”导出
        if (!Array.isArray(splitFiles.value) || splitFiles.value.length === 0) {
          errorMessage.value = '当前没有拆分文件信息，请重新获取数据（后端需返回 split_files）';
          generating.value = false;
          return;
        }

        // showDirectoryPicker：让用户选择“保存位置（目录）”
        if (!('showDirectoryPicker' in window)) {
          errorMessage.value = '当前浏览器不支持文件夹选择器（showDirectoryPicker），请使用新版 Chrome/Edge 桌面端';
          generating.value = false;
          return;
        }

        // 选择目录
        let dirHandle;
        try {
          dirHandle = await window.showDirectoryPicker();
        } catch (e) {
          // 用户取消
          generating.value = false;
          return;
        }

        // 在用户选择的目录下创建 split 文件夹
        const splitDirHandle = await dirHandle.getDirectoryHandle('split', { create: true });

        // 如果后端还没返回 xlsx 信息（旧后端/旧数据），先触发一次批量导出，再刷新 split 列表
        const shouldTryExportXlsx = selectedProducts.value.some(v => {
          const f = splitFiles.value.find(x => x && x.variety === v);
          if (!f) return false;
          // 列表里没有 xlsx_file_name，但有 csv 文件名：大概率还没生成/没返回 xlsx
          return !f.xlsx_file_name && !!(f.file_name && /\.csv$/i.test(f.file_name));
        });

        if (shouldTryExportXlsx) {
          try {
            await priceDataService.exportSplitXlsx();
            // 刷新 split 列表，拿到 xlsx_file_name/download_xlsx_url 等字段
            const refreshed = await priceDataService.listSplitFiles();
            if (refreshed && Array.isArray(refreshed.files)) {
              splitFiles.value = refreshed.files;
            }
          } catch (e) {
            // 导出接口失败不阻断流程，后面会回退下载 CSV
          }
        }

        // variety -> 优先 xlsx_file_name，否则回退 file_name
        const fileNameByVariety = new Map();
        splitFiles.value.forEach(f => {
          if (!f || !f.variety || !f.file_name) return;
          if (!fileNameByVariety.has(f.variety)) {
            const xlsxName = f.xlsx_file_name || (f.file_name ? f.file_name.replace(/\.csv$/i, '.xlsx') : null);
            fileNameByVariety.set(f.variety, xlsxName || f.file_name);
          }
        });

        // 写入每个选中的 split 文件（优先 xlsx）
        for (const variety of selectedProducts.value) {
          const preferredName = fileNameByVariety.get(variety);
          const fallbackName = preferredName && preferredName.toLowerCase().endsWith('.xlsx')
            ? preferredName.replace(/\.xlsx$/i, '.csv')
            : preferredName;

          const candidates = [];
          if (preferredName) candidates.push(preferredName);
          if (fallbackName && fallbackName !== preferredName) candidates.push(fallbackName);

          let saved = false;
          for (const name of candidates) {
            try {
              const blob = await priceDataService.downloadCsvFile(name, { scope: 'split' });
              const fileHandle = await splitDirHandle.getFileHandle(name, { create: true });
              const writable = await fileHandle.createWritable();
              await writable.write(new Uint8Array(await blob.arrayBuffer()));
              await writable.close();
              saved = true;
              break;
            } catch (e) {
              // 尝试下一个候选
            }
          }

          if (!saved) {
            logger.warn('PRICE_DATA', '保存拆分文件失败（xlsx/csv均失败）', { variety, preferredName });
          }
        }

        logger.info('PRICE_DATA', 'split文件夹保存成功', {
          selectedProducts: selectedProducts.value.length
        });
        alert('已保存：split 文件夹（优先保存选中的拆分XLSX；若缺失则回退CSV）');

        // 重置状态
        step.value = 1;
        startTime.value = '';
        endTime.value = '';
        productName.value = '';
        csvData.value = [];
        productList.value = [];
        selectedProducts.value = [];
        selectAll.value = false;
        splitFiles.value = [];

      } catch (error) {
        logger.error('PRICE_DATA', '保存split文件夹失败', {}, error);
        errorMessage.value = error.message || '保存split文件夹失败，请稍后重试';
      } finally {
        generating.value = false;
      }
    };

    // 初始化：设置默认日期为今天
    onMounted(() => {
      const today = new Date();
      endTime.value = today.toISOString().split('T')[0];
      
      // 默认开始时间为7天前
      const sevenDaysAgo = new Date(today);
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
      startTime.value = sevenDaysAgo.toISOString().split('T')[0];
    });

    return {
      step,
      startTime,
      endTime,
      productName,
      loading,
      generating,
      errorMessage,
      productList,
      selectedProducts,
      selectAll,
      maxDate,
      canQuery,
      goBack,
      fetchPriceData,
      handleSelectAll,
      exportSplitFolder
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.price-data-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}

/* 顶部导航栏 */
.header {
  background: var(--white);
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 8px rgba(107, 70, 193, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
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
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-back:hover {
  background: var(--gray-100);
  border-color: var(--primary-light);
  color: var(--primary);
}

.back-icon {
  font-size: 1.125rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary);
  margin: 0;
}

/* 主内容区域 */
.main-content {
  padding: 2rem;
  max-width: 800px;
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
  margin: 0 0 1.5rem 0;
}

.section-desc {
  font-size: 0.9375rem;
  color: var(--gray-500);
  margin: 0 0 1.5rem 0;
}

/* 表单样式 */
.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #1a202c;
  margin-bottom: 0.5rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.9375rem;
  transition: all 0.2s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

/* 错误消息 */
.error-message {
  padding: 0.75rem;
  background: #fee2e2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

/* 产品选择区域 */
.product-selection {
  margin-bottom: 2rem;
}

.selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: var(--gray-50);
  border-radius: 8px;
  margin-bottom: 1rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 500;
  color: #1a202c;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.selected-count {
  font-size: 0.875rem;
  color: var(--gray-600);
}

.product-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
  max-height: 400px;
  overflow-y: auto;
  padding: 1rem;
  border: 1px solid var(--gray-200);
  border-radius: 8px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.product-item:hover {
  background: var(--gray-50);
}

.product-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}

.btn-primary {
  padding: 0.75rem 2rem;
  background: var(--primary);
  color: var(--white);
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.3);
}

.btn-primary:disabled {
  background: var(--gray-300);
  color: var(--gray-500);
  cursor: not-allowed;
}

.btn-secondary {
  padding: 0.75rem 2rem;
  background: var(--white);
  color: var(--gray-600);
  border: 1px solid var(--gray-300);
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: var(--gray-50);
  border-color: var(--primary-light);
  color: var(--primary);
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .product-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .product-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }

  .step-section {
    padding: 1.5rem;
  }

  .product-list {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-buttons {
    flex-direction: column;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
  }
}
</style>


