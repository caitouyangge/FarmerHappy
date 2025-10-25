/**
 * 统一的前端日志工具
 * 支持不同级别的日志输出和格式化
 */

const LOG_LEVELS = {
  DEBUG: 0,
  INFO: 1,
  WARN: 2,
  ERROR: 3,
  OFF: 4
};

class Logger {
  constructor() {
    // 根据环境设置日志级别
    this.level = process.env.NODE_ENV === 'production' ? LOG_LEVELS.WARN : LOG_LEVELS.DEBUG;
    this.enableTimestamp = true;
    this.enableStackTrace = true;
    
    // 初始化时输出日志系统状态
    console.log(`%c[LOGGER INITIALIZED]`, 'color: #00ff00; font-weight: bold;');
    console.log(`Environment: ${process.env.NODE_ENV || 'undefined'}`);
    console.log(`Log Level: ${this.getLevelName()} (${this.level})`);
    console.log(`Timestamp: ${this.enableTimestamp ? 'Enabled' : 'Disabled'}`);
  }
  
  /**
   * 获取当前日志级别名称
   */
  getLevelName() {
    for (const [name, value] of Object.entries(LOG_LEVELS)) {
      if (value === this.level) {
        return name;
      }
    }
    return 'UNKNOWN';
  }

  /**
   * 格式化时间戳
   */
  getTimestamp() {
    const now = new Date();
    return now.toISOString();
  }

  /**
   * 格式化日志消息
   */
  formatMessage(level, category, message, data) {
    const parts = [];
    
    if (this.enableTimestamp) {
      parts.push(`[${this.getTimestamp()}]`);
    }
    
    parts.push(`[${level}]`);
    
    if (category) {
      parts.push(`[${category}]`);
    }
    
    parts.push(message);
    
    return parts.join(' ');
  }

  /**
   * 通用日志输出方法
   */
  log(level, category, message, data, error) {
    if (this.level > LOG_LEVELS[level.toUpperCase()]) {
      return;
    }

    const formattedMessage = this.formatMessage(level, category, message, data);
    
    switch (level.toUpperCase()) {
      case 'DEBUG':
        console.debug(formattedMessage, data || '');
        break;
      case 'INFO':
        console.info(formattedMessage, data || '');
        break;
      case 'WARN':
        console.warn(formattedMessage, data || '');
        break;
      case 'ERROR':
        console.error(formattedMessage, data || '');
        if (error && this.enableStackTrace) {
          console.error('Stack trace:', error);
        }
        break;
      default:
        console.log(formattedMessage, data || '');
    }
  }

  /**
   * DEBUG级别日志
   */
  debug(category, message, data) {
    this.log('DEBUG', category, message, data);
  }

  /**
   * INFO级别日志
   */
  info(category, message, data) {
    this.log('INFO', category, message, data);
  }

  /**
   * WARN级别日志
   */
  warn(category, message, data) {
    this.log('WARN', category, message, data);
  }

  /**
   * ERROR级别日志
   */
  error(category, message, data, error) {
    this.log('ERROR', category, message, data, error);
  }

  /**
   * API请求日志
   */
  apiRequest(method, url, data) {
    this.info('API', `${method} ${url}`, data);
  }

  /**
   * API响应日志
   */
  apiResponse(method, url, status, data) {
    this.info('API', `${method} ${url} - ${status}`, data);
  }

  /**
   * API错误日志
   */
  apiError(method, url, error) {
    this.error('API', `${method} ${url} - Failed`, {
      message: error.message || error,
      status: error.status,
      data: error.data
    }, error);
  }

  /**
   * 用户行为日志
   */
  userAction(action, details) {
    this.info('USER_ACTION', action, details);
  }

  /**
   * 组件生命周期日志
   */
  lifecycle(component, phase, data) {
    this.debug('LIFECYCLE', `${component} - ${phase}`, data);
  }

  /**
   * 表单验证日志
   */
  validation(formName, isValid, errors) {
    if (isValid) {
      this.debug('VALIDATION', `${formName} - Valid`);
    } else {
      this.warn('VALIDATION', `${formName} - Invalid`, errors);
    }
  }

  /**
   * 路由导航日志
   */
  navigation(from, to) {
    this.info('NAVIGATION', `${from} -> ${to}`);
  }

  /**
   * 设置日志级别
   */
  setLevel(level) {
    if (LOG_LEVELS[level.toUpperCase()] !== undefined) {
      this.level = LOG_LEVELS[level.toUpperCase()];
      console.log(`%c[LOGGER] Log level set to ${level.toUpperCase()} (${this.level})`, 'color: #00ff00; font-weight: bold;');
    } else {
      console.warn(`[LOGGER] Invalid log level: ${level}. Available levels: DEBUG, INFO, WARN, ERROR, OFF`);
    }
  }

  /**
   * 启用/禁用时间戳
   */
  setTimestamp(enabled) {
    this.enableTimestamp = enabled;
  }

  /**
   * 启用/禁用堆栈跟踪
   */
  setStackTrace(enabled) {
    this.enableStackTrace = enabled;
  }
  
  /**
   * 显示当前日志配置
   */
  showConfig() {
    console.log(`%c[LOGGER CONFIG]`, 'color: #00ff00; font-weight: bold;');
    console.log(`Environment: ${process.env.NODE_ENV || 'undefined'}`);
    console.log(`Current Level: ${this.getLevelName()} (${this.level})`);
    console.log(`Timestamp: ${this.enableTimestamp ? 'Enabled' : 'Disabled'}`);
    console.log(`Stack Trace: ${this.enableStackTrace ? 'Enabled' : 'Disabled'}`);
    console.log(`\nAvailable Levels:`, LOG_LEVELS);
    console.log(`\nTo change log level, use: window.$logger.setLevel('DEBUG')`);
  }
}

// 导出单例
const logger = new Logger();

// 将logger挂载到window对象，方便调试
// 在开发环境下可以通过 window.$logger 访问
if (typeof window !== 'undefined') {
  window.$logger = logger;
  console.log(`%c[LOGGER] Available globally as window.$logger`, 'color: #00ff00;');
  console.log(`%c[LOGGER] Use window.$logger.showConfig() to see current configuration`, 'color: #00ff00;');
}

export default logger;

