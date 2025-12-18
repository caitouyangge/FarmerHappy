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
        <p class="section-desc">根据查询条件，共找到 {{ productList.length }} 种不同的产品</p>

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
            @click="generateExcel">
            {{ generating ? '生成中...' : '生成Excel文件' }}
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
import * as XLSX from 'xlsx';
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

        // 下载并解析CSV文件
        await downloadAndParseCsv(csvFileName.value);

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

    // 生成Excel文件
    const generateExcel = async () => {
      if (selectedProducts.value.length === 0) {
        errorMessage.value = '请至少选择一个产品';
        return;
      }

      generating.value = true;
      errorMessage.value = '';

      try {
        // 筛选选中的产品数据
        const filteredData = csvData.value.filter(row => {
          return selectedProducts.value.includes(row['品名']);
        });

        if (filteredData.length === 0) {
          errorMessage.value = '没有找到匹配的数据';
          generating.value = false;
          return;
        }

        logger.info('PRICE_DATA', '开始生成Excel文件', {
          selectedProducts: selectedProducts.value.length,
          dataRows: filteredData.length
        });

        // 创建工作簿
        const wb = XLSX.utils.book_new();

        // 如果只选择了一个产品，直接使用产品名作为工作表名
        // 如果选择了多个产品，使用"价格数据"作为工作表名
        const sheetName = selectedProducts.value.length === 1 
          ? selectedProducts.value[0].substring(0, 31) // Excel工作表名最多31个字符
          : '价格数据';

        // 将数据转换为工作表
        const ws = XLSX.utils.json_to_sheet(filteredData);

        // 设置列宽
        const colWidths = [
          { wch: 12 }, // 一级分类
          { wch: 12 }, // 二级分类
          { wch: 15 }, // 品名
          { wch: 10 }, // 最低价
          { wch: 10 }, // 平均价
          { wch: 10 }, // 最高价
          { wch: 10 }, // 规格
          { wch: 12 }, // 产地
          { wch: 8 },  // 单位
          { wch: 20 }  // 发布日期
        ];
        ws['!cols'] = colWidths;

        // 添加工作表到工作簿
        XLSX.utils.book_append_sheet(wb, ws, sheetName);

        // 生成文件名
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
        const fileName = `农产品价格数据_${timestamp}.xlsx`;

        // 尝试使用 File System Access API 让用户选择保存位置
        if ('showSaveFilePicker' in window) {
          try {
            // 使用 File System Access API
            const fileHandle = await window.showSaveFilePicker({
              suggestedName: fileName,
              types: [{
                description: 'Excel文件',
                accept: {
                  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx']
                }
              }]
            });

            // 将工作簿转换为ArrayBuffer
            const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' });
            
            // 创建可写流并写入文件
            const writable = await fileHandle.createWritable();
            await writable.write(new Uint8Array(wbout));
            await writable.close();

            logger.info('PRICE_DATA', 'Excel文件保存成功', { fileName: fileHandle.name });
            alert(`Excel文件已保存：${fileHandle.name}`);
          } catch (error) {
            // 如果用户取消选择，不显示错误
            if (error.name !== 'AbortError') {
              logger.error('PRICE_DATA', '保存文件失败', {}, error);
              // 回退到传统下载方式
              XLSX.writeFile(wb, fileName);
              alert(`Excel文件已生成并下载：${fileName}`);
            } else {
              // 用户取消了保存
              generating.value = false;
              return;
            }
          }
        } else {
          // 浏览器不支持 File System Access API，使用传统下载方式
          // 注意：传统方式会使用浏览器的默认下载位置，用户可以在浏览器设置中配置
          XLSX.writeFile(wb, fileName);
          logger.info('PRICE_DATA', 'Excel文件生成成功（传统下载）', { fileName });
          alert(`Excel文件已生成并下载：${fileName}\n\n提示：您可以在浏览器设置中配置默认下载位置`);
        }

        // 重置状态
        step.value = 1;
        startTime.value = '';
        endTime.value = '';
        productName.value = '';
        csvData.value = [];
        productList.value = [];
        selectedProducts.value = [];
        selectAll.value = false;

      } catch (error) {
        logger.error('PRICE_DATA', '生成Excel文件失败', {}, error);
        errorMessage.value = '生成Excel文件失败，请稍后重试';
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
      generateExcel
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
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
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
@media (max-width: 768px) {
  .main-content {
    padding: 1rem;
  }

  .step-section {
    padding: 1.5rem;
  }

  .product-list {
    grid-template-columns: 1fr;
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

