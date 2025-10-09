/**
 * 前端日志工具类
 * 
 * 🎯 功能：
 * 1. 统一的日志格式
 * 2. 日志级别控制
 * 3. 自动上报错误到后端
 * 4. 本地日志缓存
 * 5. 全局错误捕获
 * 
 * 📊 使用方式：
 * import logger from '@/utils/logger'
 * logger.debug('调试信息', data)
 * logger.info('普通信息', data)
 * logger.warn('警告信息', data)
 * logger.error('错误信息', error, data)
 */

enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3
}

interface LogEntry {
  timestamp: string
  level: string
  message: string
  args: any[]
  error?: {
    name: string
    message: string
    stack?: string
  }
  url: string
  userAgent: string
}

class Logger {
  private static instance: Logger
  private logLevel: LogLevel = LogLevel.DEBUG
  private logs: LogEntry[] = []
  private maxLogs: number = 1000

  private constructor() {
    // 监听全局错误
    this.setupGlobalErrorHandler()
  }

  public static getInstance(): Logger {
    if (!Logger.instance) {
      Logger.instance = new Logger()
    }
    return Logger.instance
  }

  /**
   * 设置日志级别
   */
  public setLogLevel(level: LogLevel) {
    this.logLevel = level
  }

  /**
   * DEBUG级别日志（详细的调试信息）
   */
  public debug(message: string, ...args: any[]) {
    if (this.logLevel <= LogLevel.DEBUG) {
      this.log('DEBUG', message, args)
      console.debug(`[DEBUG] ${message}`, ...args)
    }
  }

  /**
   * INFO级别日志（重要的业务信息）
   */
  public info(message: string, ...args: any[]) {
    if (this.logLevel <= LogLevel.INFO) {
      this.log('INFO', message, args)
      console.log(`[INFO] ${message}`, ...args)
    }
  }

  /**
   * WARN级别日志（潜在问题）
   */
  public warn(message: string, ...args: any[]) {
    if (this.logLevel <= LogLevel.WARN) {
      this.log('WARN', message, args)
      console.warn(`[WARN] ${message}`, ...args)
    }
  }

  /**
   * ERROR级别日志（错误，自动上报到后端）
   */
  public error(message: string, error?: Error | any, ...args: any[]) {
    if (this.logLevel <= LogLevel.ERROR) {
      const logData = this.log('ERROR', message, args, error)
      console.error(`[ERROR] ${message}`, error, ...args)
      
      // 错误自动上报到后端（生产环境）
      if (process.env.NODE_ENV === 'production') {
        this.reportError(logData)
      }
    }
  }

  /**
   * 记录日志
   */
  private log(level: string, message: string, args: any[], error?: Error | any): LogEntry {
    const logEntry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      message,
      args,
      error: error ? {
        name: error.name || 'Error',
        message: error.message || String(error),
        stack: error.stack
      } : undefined,
      url: window.location.href,
      userAgent: navigator.userAgent
    }

    // 添加到日志缓存
    this.logs.push(logEntry)
    
    // 限制日志数量（避免内存占用过大）
    if (this.logs.length > this.maxLogs) {
      this.logs.shift()
    }

    // 保存最近100条日志到localStorage
    try {
      const recentLogs = this.logs.slice(-100)
      localStorage.setItem('xc_app_logs', JSON.stringify(recentLogs))
    } catch (e) {
      // localStorage可能已满，忽略错误
    }

    return logEntry
  }

  /**
   * 上报错误到后端
   * 使用beacon API确保页面关闭时也能发送
   */
  private reportError(logData: LogEntry) {
    try {
      const url = '/api/log/frontend-error'
      const data = JSON.stringify(logData)
      
      if (navigator.sendBeacon) {
        // 使用Beacon API（最可靠）
        const blob = new Blob([data], { type: 'application/json' })
        navigator.sendBeacon(url, blob)
      } else {
        // 降级到fetch
        fetch(url, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: data,
          keepalive: true
        }).catch(() => {
          // 上报失败也不影响主流程
        })
      }
    } catch (e) {
      // 上报失败不影响主流程
    }
  }

  /**
   * 设置全局错误监听器
   */
  private setupGlobalErrorHandler() {
    // 监听未捕获的JavaScript错误
    window.addEventListener('error', (event) => {
      this.error('全局未捕获错误', event.error, {
        message: event.message,
        filename: event.filename,
        lineno: event.lineno,
        colno: event.colno
      })
    })

    // 监听未处理的Promise Rejection
    window.addEventListener('unhandledrejection', (event) => {
      this.error('未处理的Promise Rejection', new Error(String(event.reason)), {
        reason: event.reason
      })
    })
  }

  /**
   * 获取所有日志（用于调试）
   */
  public getLogs(): LogEntry[] {
    return this.logs
  }

  /**
   * 清空日志
   */
  public clearLogs() {
    this.logs = []
    try {
      localStorage.removeItem('xc_app_logs')
    } catch (e) {
      // 忽略错误
    }
  }

  /**
   * 导出日志（用于问题报告）
   */
  public exportLogs(): string {
    return JSON.stringify(this.logs, null, 2)
  }
}

// 导出单例
export default Logger.getInstance()
export { LogLevel }

