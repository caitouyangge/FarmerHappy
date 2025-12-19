<template>
  <div class="appointment-container">
    <header class="header">
      <button class="btn-back" @click="goBack"><span>←</span> 返回</button>
      <h1 class="title">专家预约</h1>
    </header>

    <main class="main-content">
      <section v-if="isFarmer" class="farmer-section">
        <h2 class="section-title">发起预约</h2>
        <div class="form-row">
          <label class="label">预约方式</label>
          <div class="radio-group">
            <label><input type="radio" value="online" v-model="form.mode" /> 线上预约</label>
            <label><input type="radio" value="offline" v-model="form.mode" /> 线下预约</label>
          </div>
        </div>
        <div class="form-row">
          <label class="label">预约说明</label>
          <textarea v-model="form.message" class="input" rows="3" placeholder="请输入预约说明"></textarea>
        </div>
        <div class="form-row">
          <label class="label">选择专家</label>
          <div class="experts-list">
            <div v-for="ex in experts" :key="ex.expert_id" class="expert-item">
              <label>
                <input type="checkbox" :value="ex.expert_id" v-model="selectedExperts" />
                <span class="expert-name">{{ ex.nickname }}（{{ ex.expertise_field }}）</span>
                <span class="expert-meta">经验 {{ ex.work_experience || 0 }} 年 • 咨询费 ¥{{ ex.consultation_fee || 0 }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="actions">
          <button class="btn-primary" @click="submitAppointment" :disabled="submitting">{{ submitting ? '提交中...' : '提交预约' }}</button>
        </div>

        <h2 class="section-title">我的预约申请</h2>
        <div v-if="farmerAppointments.length === 0" class="empty">暂无预约记录</div>
        <div v-else class="list">
          <div class="card" v-for="item in farmerAppointments" :key="item.appointment_id">
            <div class="card-header">
              <div class="card-title">预约给：{{ item.expert_name }}（{{ item.expertise_field }}）</div>
              <div class="status" :class="'status-' + item.status">{{ statusLabel(item.status) }}</div>
            </div>
            <div class="card-body">
              <div class="row"><span class="label">方式</span><span class="value">{{ modeLabel(item.mode) }}</span></div>
              <div class="row"><span class="label">说明</span><span class="value">{{ item.message || '—' }}</span></div>
              <div class="row"><span class="label">专家补充</span><span class="value">{{ item.expert_note || '—' }}</span></div>
              <div class="row" v-if="item.scheduled_time"><span class="label">预约时间</span><span class="value">{{ formatTime(item.scheduled_time) }}</span></div>
              <div class="row" v-if="item.location"><span class="label">地点</span><span class="value">{{ item.location }}</span></div>
            </div>
          </div>
        </div>
      </section>

      <section v-else class="expert-section">
        <h2 class="section-title">收到的预约请求</h2>
        <div v-if="expertAppointments.length === 0" class="empty">暂无预约请求</div>
        <div v-else class="list">
          <div class="card" v-for="item in expertAppointments" :key="item.appointment_id">
            <div class="card-header">
              <div class="card-title">来自农户：{{ item.farmer_name }}（{{ item.farmer_phone }}）</div>
              <div class="status" :class="'status-' + item.status">{{ statusLabel(item.status) }}</div>
            </div>
            <div class="card-body">
              <div class="row"><span class="label">方式</span><span class="value">{{ modeLabel(item.mode) }}</span></div>
              <div class="row"><span class="label">说明</span><span class="value">{{ item.message || '—' }}</span></div>
              <div class="row" v-if="item.expert_note"><span class="label">补充</span><span class="value">{{ item.expert_note }}</span></div>
              <div class="row" v-if="item.scheduled_time"><span class="label">时间</span><span class="value">{{ formatTime(item.scheduled_time) }}</span></div>
              <div class="row" v-if="item.location"><span class="label">地点</span><span class="value">{{ item.location }}</span></div>
            </div>
            <div class="card-actions" v-if="item.status === 'pending'">
              <button class="btn-success" @click="openDecision(item, 'accepted')">同意</button>
              <button class="btn-danger" @click="openDecision(item, 'rejected')">拒绝</button>
            </div>
          </div>
        </div>

        <div v-if="showDecision" class="modal">
          <div class="modal-content">
            <h3 class="modal-title">{{ decisionAction === 'accepted' ? '同意预约' : '拒绝预约' }}</h3>
            <div class="form-row">
              <label class="label">补充信息</label>
              <textarea v-model="decision.expert_note" class="input" rows="3" placeholder="可填写补充说明"></textarea>
            </div>
            <div class="form-row" v-if="decisionAction === 'accepted'">
              <label class="label">预约时间</label>
              <input type="datetime-local" v-model="decision.scheduled_time" class="input" />
            </div>
            <div class="form-row" v-if="decisionAction === 'accepted'">
              <label class="label">地点</label>
              <input type="text" v-model="decision.location" class="input" placeholder="线下预约地点（可选）" />
            </div>
            <div class="modal-actions">
              <button class="btn-secondary" @click="closeDecision">取消</button>
              <button class="btn-primary" @click="submitDecision" :disabled="deciding">{{ deciding ? '提交中...' : '提交' }}</button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { expertAppointmentService } from '../api/expertAppointment';
import logger from '../utils/logger';

export default {
  name: 'ExpertAppointment',
  setup() {
    const router = useRouter();
    const user = ref(null);
    const experts = ref([]);
    const selectedExperts = ref([]);
    const farmerAppointments = ref([]);
    const expertAppointments = ref([]);
    const submitting = ref(false);
    const deciding = ref(false);

    const form = ref({
      mode: 'online',
      message: ''
    });

    const showDecision = ref(false);
    const decisionAction = ref('accepted');
    const currentAppointment = ref(null);
    const decision = ref({ expert_note: '', scheduled_time: '', location: '' });

    const isFarmer = computed(() => user.value?.userType === 'farmer');

    const goBack = () => router.push('/home');

    const loadExperts = async () => {
      try {
        experts.value = await expertAppointmentService.getExperts();
      } catch (e) {
        alert(e);
      }
    };

    const loadLists = async () => {
      try {
        if (isFarmer.value) {
          farmerAppointments.value = await expertAppointmentService.getFarmerAppointments(user.value.phone);
        } else {
          expertAppointments.value = await expertAppointmentService.getExpertAppointments(user.value.phone);
        }
      } catch (e) {
        alert(e);
      }
    };

    const submitAppointment = async () => {
      if (selectedExperts.value.length === 0) {
        alert('请选择至少一位专家');
        return;
      }
      submitting.value = true;
      try {
        await expertAppointmentService.applyAppointment({
          farmer_phone: user.value.phone,
          mode: form.value.mode,
          expert_ids: selectedExperts.value,
          message: form.value.message
        });
        selectedExperts.value = [];
        form.value.message = '';
        await loadLists();
        alert('预约提交成功');
      } catch (e) {
        alert(e);
      } finally {
        submitting.value = false;
      }
    };

    const openDecision = (item, action) => {
      currentAppointment.value = item;
      decisionAction.value = action;
      decision.value = { expert_note: '', scheduled_time: '', location: '' };
      showDecision.value = true;
    };

    const closeDecision = () => {
      showDecision.value = false;
      currentAppointment.value = null;
    };

    const submitDecision = async () => {
      if (!currentAppointment.value) return;
      deciding.value = true;
      try {
        let scheduled = decision.value.scheduled_time;
        if (scheduled) {
          scheduled = scheduled.replace('T', ' ') + ':00';
        }
        await expertAppointmentService.decideAppointment(currentAppointment.value.appointment_id, {
          expert_phone: user.value.phone,
          action: decisionAction.value,
          expert_note: decision.value.expert_note,
          scheduled_time: scheduled || null,
          location: decision.value.location || null
        });
        await loadLists();
        closeDecision();
        alert('提交成功');
      } catch (e) {
        alert(e);
      } finally {
        deciding.value = false;
      }
    };

    const statusLabel = (s) => ({ pending: '待处理', accepted: '已同意', rejected: '已拒绝' }[s] || s);
    const modeLabel = (m) => ({ online: '线上', offline: '线下' }[m] || m);
    const formatTime = (t) => {
      try {
        const dt = new Date(t);
        const y = dt.getFullYear();
        const mm = String(dt.getMonth() + 1).padStart(2, '0');
        const d = String(dt.getDate()).padStart(2, '0');
        const h = String(dt.getHours()).padStart(2, '0');
        const m = String(dt.getMinutes()).padStart(2, '0');
        return `${y}-${mm}-${d} ${h}:${m}`;
      } catch { return t; }
    };

    onMounted(async () => {
      try {
        const stored = localStorage.getItem('user');
        user.value = stored ? JSON.parse(stored) : null;
        if (!user.value) {
          router.push('/login');
          return;
        }
        if (isFarmer.value) await loadExperts();
        await loadLists();
      } catch (e) {
        logger.error('APPOINTMENT', '初始化失败', {}, e);
      }
    });

    return {
      goBack,
      isFarmer,
      experts,
      selectedExperts,
      farmerAppointments,
      expertAppointments,
      form,
      submitting,
      submitAppointment,
      statusLabel,
      modeLabel,
      formatTime,
      showDecision,
      decisionAction,
      decision,
      openDecision,
      closeDecision,
      submitDecision,
      deciding
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.appointment-container { min-height: 100vh; background: var(--bg-gradient); }
.header { display: flex; align-items: center; padding: 1rem 1.5rem; background: var(--white); border-bottom: 1px solid var(--gray-200); }
.btn-back { background: transparent; border: none; color: var(--primary); cursor: pointer; font-size: 0.95rem; }
.title { margin: 0 auto; font-size: 1.25rem; font-weight: 600; color: var(--gray-900); }
.main-content { padding: 1rem; max-width: 960px; margin: 0 auto; }
.section-title { font-size: 1.1rem; font-weight: 600; margin: 1rem 0; color: var(--gray-800); }
.form-row { display: flex; flex-direction: column; gap: 0.5rem; margin: 0.75rem 0; }
.label { color: var(--gray-600); }
.input { padding: 0.5rem 0.75rem; border: 1px solid var(--gray-300); border-radius: 8px; }
.radio-group { display: flex; gap: 1rem; }
.experts-list { display: grid; grid-template-columns: 1fr; gap: 0.5rem; border: 1px dashed var(--gray-300); padding: 0.75rem; border-radius: 8px; }
.expert-item { display: flex; align-items: center; }
.expert-name { margin-left: 0.5rem; font-weight: 600; color: var(--gray-900); }
.expert-meta { margin-left: 0.5rem; color: var(--gray-500); font-size: 0.85rem; }
.actions { margin-top: 0.75rem; }
.btn-primary { background: var(--primary); color: var(--white); border: none; border-radius: 8px; padding: 0.5rem 1rem; cursor: pointer; }
.btn-success { background: #22c55e; color: var(--white); border: none; border-radius: 8px; padding: 0.4rem 0.8rem; cursor: pointer; }
.btn-danger { background: #ef4444; color: var(--white); border: none; border-radius: 8px; padding: 0.4rem 0.8rem; cursor: pointer; }
.btn-secondary { background: var(--gray-300); color: var(--gray-900); border: none; border-radius: 8px; padding: 0.4rem 0.8rem; cursor: pointer; }
.list { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
.card { background: var(--white); border: 1px solid var(--gray-200); border-radius: 12px; padding: 0.75rem; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-weight: 600; color: var(--gray-900); }
.card-body { display: grid; gap: 0.25rem; margin-top: 0.5rem; }
.row { display: flex; gap: 0.5rem; }
.row .label { width: 5rem; color: var(--gray-600); }
.row .value { flex: 1; color: var(--gray-800); }
.status { padding: 0.2rem 0.5rem; border-radius: 999px; font-size: 0.8rem; }
.status-pending { background: #fff7ed; color: #c2410c; }
.status-accepted { background: #ecfeff; color: #0e7490; }
.status-rejected { background: #fee2e2; color: #b91c1c; }
.empty { color: var(--gray-500); padding: 0.5rem; }
.modal { position: fixed; inset: 0; background: rgba(0,0,0,0.25); display: flex; align-items: center; justify-content: center; }
.modal-content { width: 520px; background: var(--white); border-radius: 12px; padding: 1rem; }
.modal-title { font-weight: 600; color: var(--gray-900); margin-bottom: 0.5rem; }
.modal-actions { display: flex; justify-content: flex-end; gap: 0.5rem; margin-top: 0.75rem; }

@media (max-width: 768px) {
  .modal-content { width: 92%; }
}
</style>