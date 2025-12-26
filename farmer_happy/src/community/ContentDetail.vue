<template>
  <div class="content-detail-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <button class="btn-back" @click="goBack">
        <span class="back-icon">←</span>
        返回
      </button>
      <h1 class="header-title">内容详情</h1>
      <div class="header-placeholder"></div>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 内容详情 -->
    <div v-else-if="contentDetail" class="detail-wrapper">
      <main class="main-content">
        <!-- 内容主体 -->
        <article class="content-article">
          <div class="content-header">
            <div class="content-type-badge" :class="getTypeClass(contentDetail.content_type)">
              {{ getTypeLabel(contentDetail.content_type) }}
            </div>
            <div class="content-meta">
              <span class="author">
                <span class="author-icon">👤</span>
                {{ contentDetail.author_name || contentDetail.author_nickname }}
              </span>
              <span class="role-badge" :class="getRoleClass(contentDetail.author_role)">
                {{ getRoleLabel(contentDetail.author_role) }}
              </span>
              <span class="time">{{ formatTime(contentDetail.created_at) }}</span>
            </div>
          </div>

          <h1 class="article-title">{{ contentDetail.title }}</h1>

          <div class="article-content">{{ contentDetail.content }}</div>

          <div v-if="contentDetail.images && contentDetail.images.length > 0" class="article-images">
            <img
              v-for="(image, idx) in contentDetail.images"
              :key="idx"
              :src="image"
              :alt="`图片${idx + 1}`"
              class="article-image"
              @click="previewImage(image, contentDetail.images)"
            />
          </div>

          <div class="article-stats">
            <span class="stat-item">
              <span class="stat-icon">👁</span>
              {{ contentDetail.view_count || 0 }} 浏览
            </span>
            <span class="stat-item">
              <span class="stat-icon">💬</span>
              {{ contentDetail.comment_count || 0 }} 评论
            </span>
          </div>
        </article>

        <!-- 评论区域 -->
        <section class="comments-section">
          <div class="section-header">
            <h2 class="section-title">
              <span class="title-icon">💬</span>
              评论 ({{ comments.length }})
            </h2>
          </div>

          <!-- 发表评论表单 -->
          <div class="comment-form">
            <textarea
              v-model="newComment"
              class="comment-input"
              placeholder="写下你的评论..."
              rows="3"
            ></textarea>
            <div class="comment-actions">
              <button class="btn-cancel" @click="cancelComment">取消</button>
              <button class="btn-submit" @click="handlePostComment" :disabled="posting">
                {{ posting ? '提交中...' : '发表评论' }}
              </button>
            </div>
          </div>

          <!-- 评论列表 -->
          <div class="comments-list" v-if="comments.length > 0">
            <div
              v-for="(comment, index) in filteredComments"
              :key="comment?.comment_id || `comment-${index}`"
              class="comment-item"
            >
              <div class="comment-header">
                <div class="comment-author">
                  <span class="author-avatar">{{ getAuthorInitial(comment?.author_nickname) }}</span>
                  <div class="author-info">
                    <span class="author-name">{{ comment?.author_nickname || '匿名用户' }}</span>
                    <span class="author-role-badge" :class="getRoleClass(comment?.author_role)">
                      {{ getRoleLabel(comment?.author_role) }}
                    </span>
                  </div>
                </div>
                <span class="comment-time">{{ formatTime(comment?.created_at) }}</span>
              </div>

              <div class="comment-content">{{ comment?.content || '' }}</div>

              <!-- 回复按钮 -->
              <button class="btn-reply" @click="toggleReplyForm(comment?.comment_id)" v-if="comment?.comment_id">
                <span class="reply-icon">↩</span>
                回复
              </button>

              <!-- 回复表单 -->
              <div v-if="getReplyFormStatus(comment?.comment_id)" class="reply-form">
                <textarea
                  v-model="replyContents[comment.comment_id]"
                  class="reply-input"
                  placeholder="写下你的回复..."
                  rows="2"
                ></textarea>
                <div class="reply-actions">
                  <button class="btn-cancel" @click="cancelReply(comment.comment_id)">取消</button>
                  <button
                    class="btn-submit"
                    @click="handlePostReply(comment.comment_id)"
                    :disabled="getReplyingStatus(comment?.comment_id)"
                  >
                    {{ getReplyingStatus(comment?.comment_id) ? '回复中...' : '发表回复' }}
                  </button>
                </div>
              </div>

              <!-- 回复列表 -->
              <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                  <div
                    v-for="(reply, replyIndex) in getFilteredReplies(comment.replies)"
                    :key="reply?.comment_id || `reply-${replyIndex}`"
                    class="reply-item"
                  >
                  <div class="reply-header">
                    <div class="reply-author-info">
                      <span class="reply-text">
                        <span class="reply-sender">{{ reply?.author_nickname || '匿名用户' }}</span>
                        <span class="author-role-badge small" :class="getRoleClass(reply?.author_role)" v-if="reply?.author_role">
                          {{ getRoleLabel(reply?.author_role) }}
                        </span>
                        <span class="reply-to" v-if="reply?.reply_to_nickname">
                          回复 @{{ reply.reply_to_nickname }}
                        </span>
                      </span>
                    </div>
                    <span class="reply-time">{{ formatTime(reply?.created_at) }}</span>
                  </div>
                  <div class="reply-content">{{ reply?.content || '' }}</div>
                  
                  <!-- 回复按钮 -->
                  <button class="btn-reply small" @click="toggleReplyForm(reply?.comment_id)" v-if="reply?.comment_id">
                    <span class="reply-icon">↩</span>
                    回复
                  </button>

                  <!-- 回复表单 -->
                  <div v-if="getReplyFormStatus(reply?.comment_id)" class="reply-form">
                    <textarea
                      v-model="replyContents[reply.comment_id]"
                      class="reply-input"
                      :placeholder="`回复 @${reply.author_nickname}...`"
                      rows="2"
                    ></textarea>
                    <div class="reply-actions">
                      <button class="btn-cancel" @click="cancelReply(reply.comment_id)">取消</button>
                      <button
                        class="btn-submit"
                        @click="handlePostReply(reply.comment_id)"
                        :disabled="getReplyingStatus(reply?.comment_id)"
                      >
                        {{ getReplyingStatus(reply?.comment_id) ? '回复中...' : '发表回复' }}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 无评论状态 -->
          <div v-else class="empty-comments">
            <div class="empty-icon">💭</div>
            <p class="empty-text">还没有评论，快来抢沙发吧！</p>
          </div>
        </section>
      </main>
    </div>

    <!-- 图片预览模态框 -->
    <div v-if="showImagePreview" class="image-preview-modal">
      <img :src="currentImage" :alt="'预览图片'" class="preview-image" @click.stop />
      <button class="close-preview" @click="closeImagePreview">×</button>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { communityService } from '../api/community';
import logger from '../utils/logger';

export default {
  name: 'ContentDetail',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const contentDetail = ref(null);
    const comments = ref([]);
    const newComment = ref('');
    const posting = ref(false);
    const showReplyForms = reactive({});
    const replyContents = reactive({});
    const replying = reactive({});

    // 确保reactive对象的安全初始化
    const ensureCommentState = (commentId) => {
      if (!commentId) return;
      if (!(commentId in showReplyForms)) {
        showReplyForms[commentId] = false;
      }
      if (!(commentId in replyContents)) {
        replyContents[commentId] = '';
      }
      if (!(commentId in replying)) {
        replying[commentId] = false;
      }
    };
    const showImagePreview = ref(false);
    const currentImage = ref('');
    const imageList = ref([]);

    // 获取用户信息
    const getUserInfo = () => {
      try {
        const storedUser = localStorage.getItem('user');
        return storedUser ? JSON.parse(storedUser) : null;
      } catch (error) {
        logger.error('CONTENT_DETAIL', '获取用户信息失败', {}, error);
        return null;
      }
    };

    // 加载内容详情
    const loadContentDetail = async () => {
      const contentId = route.params.id;
      if (!contentId) {
        logger.error('CONTENT_DETAIL', '缺少内容ID');
        router.push('/community');
        return;
      }

      loading.value = true;
      try {
        logger.info('CONTENT_DETAIL', '加载内容详情', { contentId });
        const detail = await communityService.getContentDetail(contentId);
        contentDetail.value = detail;
        
        // 加载评论列表
        await loadComments(contentId);
      } catch (error) {
        logger.error('CONTENT_DETAIL', '加载内容详情失败', { contentId }, error);
        alert(error.message || '加载内容失败，请稍后重试');
        router.push('/community');
      } finally {
        loading.value = false;
      }
    };

    // 加载评论列表
    const loadComments = async (contentId) => {
      try {
        logger.info('CONTENT_DETAIL', '加载评论列表', { contentId });
        const data = await communityService.getCommentList(contentId);
        // 过滤掉null值和无效的评论，并确保replies也被过滤
        const rawComments = data.comments || [];
        
        comments.value = rawComments
          .filter(comment => comment && typeof comment === 'object')
          .map(comment => ({
            ...comment,
            replies: (comment.replies || []).filter(reply => 
              reply && typeof reply === 'object'
            )
          }));
        logger.info('CONTENT_DETAIL', '评论列表加载成功', { count: comments.value.length });
      } catch (error) {
        logger.error('CONTENT_DETAIL', '加载评论列表失败', { contentId }, error);
        comments.value = [];
      }
    };

    // 发表评论
    const handlePostComment = async () => {
      if (!newComment.value.trim()) {
        alert('请输入评论内容');
        return;
      }

      const userInfo = getUserInfo();
      if (!userInfo || !userInfo.phone) {
        alert('请先登录');
        router.push('/login');
        return;
      }

      posting.value = true;
      const contentId = route.params.id;

      try {
        logger.userAction('POST_COMMENT', { contentId });
        await communityService.postComment(contentId, {
          comment: newComment.value.trim(),
          phone: userInfo.phone
        });

        logger.info('CONTENT_DETAIL', '评论发表成功', { contentId });
        newComment.value = '';
        // 重新加载评论列表
        await loadComments(contentId);
      } catch (error) {
        logger.error('CONTENT_DETAIL', '发表评论失败', { contentId }, error);
        alert(error.message || '发表评论失败，请稍后重试');
      } finally {
        posting.value = false;
      }
    };

    // 取消评论
    const cancelComment = () => {
      newComment.value = '';
    };

    // 切换回复表单
    const toggleReplyForm = (commentId) => {
      if (!commentId) return;
      ensureCommentState(commentId);
      showReplyForms[commentId] = !showReplyForms[commentId];
      if (showReplyForms[commentId] && !replyContents[commentId]) {
        replyContents[commentId] = '';
      }
    };

    // 发表回复
    const handlePostReply = async (commentId) => {
      if (!commentId) return;
      ensureCommentState(commentId);
      const replyContent = replyContents[commentId];
      if (!replyContent || !replyContent.trim()) {
        alert('请输入回复内容');
        return;
      }

      const userInfo = getUserInfo();
      if (!userInfo || !userInfo.phone) {
        alert('请先登录');
        router.push('/login');
        return;
      }

      replying[commentId] = true;
      const contentId = route.params.id;

      try {
        logger.userAction('POST_REPLY', { commentId, contentId });
        await communityService.postReply(commentId, {
          comment: replyContent.trim(),
          phone: userInfo.phone
        });

        logger.info('CONTENT_DETAIL', '回复发表成功', { commentId });
        replyContents[commentId] = '';
        showReplyForms[commentId] = false;
        // 重新加载评论列表
        await loadComments(contentId);
      } catch (error) {
        logger.error('CONTENT_DETAIL', '发表回复失败', { commentId }, error);
        alert(error.message || '发表回复失败，请稍后重试');
      } finally {
        replying[commentId] = false;
      }
    };

    // 取消回复
    const cancelReply = (commentId) => {
      if (!commentId) return;
      ensureCommentState(commentId);
      replyContents[commentId] = '';
      showReplyForms[commentId] = false;
    };

    // 返回
    const goBack = () => {
      router.push('/community');
    };

    // 格式化时间
    const formatTime = (timeStr) => {
      if (!timeStr) return '';
      try {
        const date = new Date(timeStr);
        const now = new Date();
        const diff = now - date;
        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (minutes < 1) return '刚刚';
        if (minutes < 60) return `${minutes}分钟前`;
        if (hours < 24) return `${hours}小时前`;
        if (days < 7) return `${days}天前`;
        return date.toLocaleDateString('zh-CN');
      } catch (error) {
        return timeStr;
      }
    };

    // 获取作者首字母
    const getAuthorInitial = (name) => {
      if (!name) return '?';
      return name.charAt(0).toUpperCase();
    };

    // 获取类型标签
    const getTypeLabel = (type) => {
      const typeMap = {
        articles: '文章',
        questions: '提问',
        experiences: '经验分享'
      };
      return typeMap[type] || type;
    };

    // 获取类型样式类
    const getTypeClass = (type) => {
      return `type-${type}`;
    };

    // 获取角色标签
    const getRoleLabel = (role) => {
      const roleMap = {
        farmer: '农户',
        expert: '专家',
        buyer: '买家',
        bank: '银行'
      };
      return roleMap[role] || role;
    };

    // 获取角色样式类
    const getRoleClass = (role) => {
      return `role-${role}`;
    };

    // 预览图片
    const previewImage = (image, images) => {
      currentImage.value = image;
      imageList.value = images || [];
      showImagePreview.value = true;
    };

    // 关闭图片预览
    const closeImagePreview = () => {
      showImagePreview.value = false;
      currentImage.value = '';
      imageList.value = [];
    };

    // 过滤有效的评论
    const filteredComments = computed(() => {
      return (comments.value || []).filter(comment => 
        comment && typeof comment === 'object' && comment.comment_id
      );
    });

    // 过滤有效的回复 - 修复字段名问题
    const getFilteredReplies = (replies) => {
      return (replies || []).filter(reply => {
        // 实际数据中回复使用的是 comment_id 而不是 reply_id
        return reply && typeof reply === 'object' && reply.comment_id;
      });
    };

    // 安全地获取回复表单显示状态
    const getReplyFormStatus = (commentId) => {
      if (!commentId) return false;
      ensureCommentState(commentId);
      return Boolean(showReplyForms[commentId]);
    };

    // 安全地获取回复内容
    const getReplyContent = (commentId) => {
      if (!commentId) return '';
      ensureCommentState(commentId);
      return replyContents[commentId] || '';
    };

    // 安全地获取回复状态
    const getReplyingStatus = (commentId) => {
      if (!commentId) return false;
      ensureCommentState(commentId);
      return Boolean(replying[commentId]);
    };

    onMounted(() => {
      logger.lifecycle('ContentDetail', 'mounted');
      loadContentDetail();
    });

    return {
      loading,
      contentDetail,
      comments,
      filteredComments,
      newComment,
      posting,
      showReplyForms,
      replyContents,
      replying,
      showImagePreview,
      currentImage,
      handlePostComment,
      cancelComment,
      toggleReplyForm,
      handlePostReply,
      cancelReply,
      goBack,
      formatTime,
      getAuthorInitial,
      getTypeLabel,
      getTypeClass,
      getRoleLabel,
      getRoleClass,
      previewImage,
      closeImagePreview,
      getFilteredReplies,
      getReplyFormStatus,
      getReplyContent,
      getReplyingStatus
    };
  }
};
</script>

<style scoped>
@import '../assets/styles/theme.css';

.content-detail-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}

/* 顶部导航栏 */
.header {
  background: var(--white);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  border-color: var(--primary);
  color: var(--primary);
}

.header-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary);
  margin: 0;
}

.header-placeholder {
  width: 120px;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--white);
  margin: 2rem auto;
  max-width: 1200px;
  border-radius: 16px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--gray-200);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 主内容 */
.detail-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 内容文章 */
.content-article {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.content-type-badge {
  padding: 0.5rem 1rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--white);
}

.type-articles {
  background: linear-gradient(135deg, #8B5CF6, #A78BFA);
}

.type-questions {
  background: linear-gradient(135deg, #EC4899, #F472B6);
}

.type-experiences {
  background: linear-gradient(135deg, #06B6D4, #22D3EE);
}

.content-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.author {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  color: var(--gray-600);
  font-size: 0.9375rem;
}

.author-icon {
  font-size: 1.125rem;
}

.role-badge {
  padding: 0.375rem 0.875rem;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--white);
}

.role-farmer {
  background: linear-gradient(135deg, #10B981, #34D399);
}

.role-expert {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
}

.role-buyer {
  background: linear-gradient(135deg, #F59E0B, #FBBF24);
}

.role-bank {
  background: linear-gradient(135deg, #6366F1, #818CF8);
}

.time {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.article-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 1.5rem 0;
  line-height: 1.4;
}

.article-content {
  font-size: 1.125rem;
  line-height: 1.8;
  color: #374151;
  white-space: pre-wrap;
  word-wrap: break-word;
  margin-bottom: 1.5rem;
}

.article-images {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.article-image {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
  border-radius: 12px;
  cursor: pointer;
  transition: transform 0.2s;
}

.article-image:hover {
  transform: scale(1.02);
}

.article-stats {
  display: flex;
  gap: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--gray-200);
  color: var(--gray-500);
  font-size: 0.9375rem;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stat-icon {
  font-size: 1.125rem;
}

/* 评论区域 */
.comments-section {
  background: var(--white);
  padding: 2rem;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.08);
}

.section-header {
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1a202c;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.title-icon {
  font-size: 1.5rem;
}

/* 评论表单 */
.comment-form {
  margin-bottom: 2rem;
  padding-bottom: 2rem;
  border-bottom: 2px solid var(--gray-200);
}

.comment-input {
  width: 100%;
  padding: 1rem;
  border: 2px solid var(--gray-300);
  border-radius: 12px;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
  transition: all 0.2s;
}

.comment-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1rem;
}

/* 评论列表 */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.comment-item {
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--gray-200);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: var(--white);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 1rem;
}

.author-avatar.small {
  width: 32px;
  height: 32px;
  font-size: 0.875rem;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.author-name {
  font-weight: 600;
  color: #1a202c;
  font-size: 0.9375rem;
}

.author-role-badge {
  padding: 0.125rem 0.5rem;
  border-radius: 4px;
  font-size: 0.625rem;
  font-weight: 600;
  color: var(--white);
  width: fit-content;
}

.author-role-badge.small {
  padding: 0.125rem 0.375rem;
  font-size: 0.5625rem;
}

.comment-time {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.comment-content {
  color: #374151;
  line-height: 1.6;
  margin-bottom: 0.75rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.btn-reply {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  background: transparent;
  border: 1px solid var(--gray-300);
  border-radius: 6px;
  color: var(--gray-600);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-reply:hover {
  background: var(--gray-100);
  border-color: var(--primary);
  color: var(--primary);
}

.reply-icon {
  font-size: 1rem;
}

.btn-reply.small {
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
  margin-top: 0.5rem;
}

/* 回复表单 */
.reply-form {
  margin-top: 1rem;
  padding: 1rem;
  background: var(--gray-100);
  border-radius: 8px;
}

.reply-input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid var(--gray-300);
  border-radius: 8px;
  font-size: 0.9375rem;
  font-family: inherit;
  resize: vertical;
  transition: all 0.2s;
}

.reply-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(107, 70, 193, 0.1);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 0.75rem;
}

/* 回复列表 */
.replies-list {
  margin-top: 1rem;
  padding-left: 1rem;
  border-left: 3px solid var(--primary-light);
}

.reply-item {
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--gray-100);
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.reply-author {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.reply-author-info {
  display: flex;
  align-items: center;
  flex: 1;
}

.reply-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.reply-sender {
  font-weight: 600;
  color: #1a202c;
  font-size: 0.9375rem;
}

.reply-to {
  color: var(--gray-500);
  font-size: 0.875rem;
}

.reply-time {
  color: var(--gray-500);
  font-size: 0.8125rem;
}

.reply-content {
  color: #4B5563;
  line-height: 1.6;
  font-size: 0.9375rem;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 按钮样式 */
.btn-cancel,
.btn-submit {
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 8px;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: var(--gray-200);
  color: var(--gray-600);
}

.btn-cancel:hover {
  background: var(--gray-300);
}

.btn-submit {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: var(--white);
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 70, 193, 0.3);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 空评论状态 */
.empty-comments {
  text-align: center;
  padding: 3rem 2rem;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-text {
  color: var(--gray-500);
  font-size: 1rem;
}

/* 图片预览模态框 */
.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: pointer;
}

.preview-image {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
  border-radius: 8px;
}

.close-preview {
  position: absolute;
  top: 2rem;
  right: 2rem;
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  color: var(--white);
  font-size: 2rem;
  cursor: pointer;
  transition: all 0.2s;
}

.close-preview:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header {
    padding: 1rem;
  }

  .header-title {
    font-size: 1.25rem;
  }

  .detail-wrapper {
    padding: 1rem;
  }

  .content-article,
  .comments-section {
    padding: 1.5rem;
  }

  .article-title {
    font-size: 1.5rem;
  }

  .article-content {
    font-size: 1rem;
  }
}
</style>

