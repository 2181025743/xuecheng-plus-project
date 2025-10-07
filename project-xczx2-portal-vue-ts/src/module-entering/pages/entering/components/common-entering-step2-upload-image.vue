<template>
  <div>
    <el-upload
      list-type="picture-card"
      accept=".jpg, .png, .bmp"
      :file-list="fileList"
      :before-upload="handleBeforeUpload"
      :on-success="handleUploadSuccess"
      :on-error="handleUploadError"
      action="/api/media/upload/coursefile"
      name="filedata"
      :on-preview="handleOnPreview"
      :on-remove="handleOnRemove"
      :class="{disabled:uploadDisabled}"
    >
      <i class="el-icon-plus"></i>
      <div class="el-upload__tip" slot="tip" style="line-height: 20px;">
        <slot></slot>
        <br />文件小于2M
        <br />支持JPG/PNG/BMP等格式图片
      </div>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible">
      <img width="100%" :src="syncedImageUrl" alt />
    </el-dialog>
  </div>
</template>

<script lang="ts">
import { Component, Vue, PropSync, Watch } from 'vue-property-decorator'
import {
  ElUploadInternalRawFile,
  ElUploadInternalFileDetail,
  HttpRequestOptions
} from 'element-ui/types/upload'
import * as qiniu from 'qiniu-js'
import { IQnParamsDTO } from '@/entity/media-page-list'
import { getQnParams } from '@/api/common'

@Component
export default class CommonEnteringStep2UploadImage extends Vue {
  private dialogVisible: boolean = false

  @PropSync('imageUrl', { type: String, required: false, default: '' })
  syncedImageUrl!: string

  @Watch('syncedImageUrl')
  onImageUrlChanged(newImageUrl: string, oldImageUrl: string) {
    // console.log('--------------onImageUrlChanged前--------------')
    // console.log(this.fileList)
    if (newImageUrl) {
      this.fileList = [{ url: newImageUrl }]
    } else {
      this.fileList = []
    }
    //解决回退到上一页路径前缀消失问题
    // if(this.syncedImageUrl.length>0 && this.syncedImageUrl.indexOf(`${process.env.VUE_APP_SERVER_PICSERVER_URL}`)<0){
      
    //   this.syncedImageUrl = `${process.env.VUE_APP_SERVER_PICSERVER_URL}`+this.syncedImageUrl;
    //   alert(this.syncedImageUrl)
    // }
    // console.log('--------------onImageUrlChanged后--------------')
    // console.log(this.fileList)
  }

  private fileList: any[] = []

  // computed
  get uploadDisabled() {
    return this.fileList.length > 0
  }

  /**
   * 上传文件之前的钩子
   * TODO: 增加其他文件格式
   */
  private handleBeforeUpload(file: ElUploadInternalRawFile) {
    const isJPG =
      file.type === 'image/jpeg' ||
      file.type === 'image/png' ||
      file.type === 'image/bmp'
    const isLt2M = file.size / 1024 / 1024 < 2

    if (!isJPG) {
      this.$message.error('上传头像图片只能是 JPG/PNG/BMP 格式!')
    }
    if (!isLt2M) {
      this.$message.error('上传头像图片大小不能超过 2MB!')
    }
    return isJPG && isLt2M
  }
  /**
   * 上传成功钩子
   */
  private handleUploadSuccess(res, file) {
    console.log('📤 上传成功，返回数据：', res)
    const objectName = (res && (res.url || res.result || res.filePath)) || ''
    console.log('📤 对象名(objectName)：', objectName)

    if (objectName) {
      // 构造完整URL，兼容环境变量缺失
      const base = process.env.VUE_APP_SERVER_PICSERVER_URL || ''
      let fullUrl = base ? `${base}${objectName}` : objectName

      this.syncedImageUrl = fullUrl
      console.log('📤 拼接后的完整URL：', this.syncedImageUrl)
      this.$message.success('图片上传成功！')

      // 通知父组件，同时传递fullUrl和objectName，便于父组件保存与展示
      this.$emit('upload-success', { fullUrl, objectName })
    } else {
      console.error('❌ 返回数据中没有对象名字段：', res)
      this.$message.error('图片上传失败：返回数据格式错误')
    }
  }
  /**
   * 文件上传失败钩子
   */

  private handleUploadError(err){
    console.log('上传失败:'+err.errMessage)
  }
  /**
   * 覆盖默认的上传行为
   */
  private handleHttpRequest(options: HttpRequestOptions) {
    let file = options.file
    let filename = file.name
    let index = filename.lastIndexOf('.')
    let suffix = filename.substr(index)

    // 文档上传到七牛云
    this.qiniuyunUpload(file)
  }

  /**
   * 文档上传到七牛云
   * TODO: 异常系考虑 401
   * TODO: 后端提供公开资源接口
   */
  private async qiniuyunUpload(file: ElUploadInternalFileDetail) {
    // 准备工作
    let qnParams: IQnParamsDTO = await getQnParams()

    // 开始上传
    let token = qnParams.qnToken
    let config = {
      useCdnDomain: true,
      region: null // 自动分析上传域名区域
    }
    let putExtra = {
      fname: '',
      params: {},
      mimeType: null
    }
    let key = qnParams.key

    let next = response => {
      let total = response.total
      let percentage = Math.ceil(total.percent)
      console.log(`媒资上传到七牛云进度...${percentage}%`)
      console.log(response)
    }
    let error = response => {
      console.log('媒资上传到七牛云失败...')
      console.log(response)
    }
    let complete = response => {
      this.syncedImageUrl = `http://${qnParams.domain}/${qnParams.key}`
      console.log('媒资上传到七牛云完成...')
      console.log(response)
    }

    let subscription
    // 调用sdk上传接口获得相应的observable，控制上传和暂停
    let observable = qiniu.upload(file, key, token, putExtra, config)
    observable.subscribe(next, error, complete)
    console.log('媒资上传到七牛云开始...')
  }

  /**
   * 点击文件列表中已上传的文件时的钩子
   */
  private handleOnPreview(file: ElUploadInternalFileDetail) {
    this.dialogVisible = true
  }

  /**
   * 文件列表移除文件时的钩子
   */
  private handleOnRemove(
    file: ElUploadInternalFileDetail,
    fileList: ElUploadInternalFileDetail[]
  ) {
    this.syncedImageUrl = ''
  }
}
</script>

<style lang="scss">
.disabled .el-upload--picture-card {
  display: none;
}
</style>