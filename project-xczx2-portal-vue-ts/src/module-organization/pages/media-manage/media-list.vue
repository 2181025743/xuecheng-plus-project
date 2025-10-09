<template>
  <div class="course-list portal-content">
    <div class="workspace">
      <div class="banner">
        <span class="primary-title">媒资管理</span>
        <el-button
          type="primary"
          size="medium"
          class="btn-add el-button"
          @click="handleOpenAddMediaDialog"
        >上传视频</el-button>
      </div>

      <!-- 搜索栏 -->
      <div class="searcher">
        <el-input
          class="el-input"
          placeholder="请输入关键词"
          suffix-icon="el-icon-search"
          v-model="listQueryData.filename"
        />
        <el-select v-model="listQueryData.fileType" placeholder="请选择文件类型">
          <el-option
            v-for="item in mediaTypes"
            :key="item.code"
            :label="item.desc"
            :value="item.code"
          ></el-option>
        </el-select>
      </div>

      <!-- 数据列表 -->
      <el-table
        class="dataList"
        v-loading="listLoading"
        :data="listResult.items"
        border
        style="width: 100%"
        :header-cell-style="{ textAlign: 'center' }"
        :default-sort="{ prop: 'createDate', order: 'descending' }"
        @sort-change="handleSortChange"
      >
        <el-table-column label="文件名称" width="400" align="center">
          <template slot-scope="scope">
            {{ scope.row.filename }}
            <!--<br />
            视频时长：{{ scope.row.timelength }}-->
          </template>
        </el-table-column>
      <el-table-column label="类型" align="center" >
          <template slot-scope="scope">
            {{ getMediaTypes(scope.row.fileType) }}
           
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" align="center"></el-table-column>

        <el-table-column label="上传时间" prop="createDate" sortable="custom" align="center">
          <template slot-scope="scope">
            {{ scope.row.username }}
            <br />
            {{ scope.row.createDate | dateTimeFormat }}
          </template>
        </el-table-column>

        <el-table-column label="审核状态" align="center">
          <template slot-scope="scope">
            {{ getMediaAuditStatus(scope.row.auditStatus) }}
            <br />
            <el-button
              v-if="scope.row.auditStatus === '002001'"
              type="text"
              @click="handleViewAuditMind(scope.row.auditMind)"
            >点击查看原因</el-button>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini"  :disabled="!scope.row.fileId && !scope.row.url" @click="handlePreview(scope.row)">预览</el-button>
          
            <el-button
              type="text"
              size="mini"
              @click="handleOpenDeleteMediaConfirm(scope.row.id)"
            >移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 翻页控制 -->
      <div class="dataList-pagination">
        <Pagination
          v-show="listResult.counts > 0"
          :total="listResult.counts"
          :page.sync="listQuery.pageNo"
          :limit.sync="listQuery.pageSize"
          @pagination="getMediaPageList"
        />
      </div>
    </div>

    <!-- 上传资料对话框 -->
    <media-add-dialog :dialogVisible.sync="dialogVisible" @closeDialog="getMediaPageList"></media-add-dialog>

  <!-- 预览遮罩层 -->
  <div v-if="previewVisible" class="xc-preview-mask" @click="previewVisible=false">
    <div class="xc-preview-box" @click.stop>
      <img v-if="previewType==='image'" :src="previewUrl" class="xc-preview-img" />
      <video v-else-if="previewType==='video'" :src="previewUrl" class="xc-preview-video" controls autoplay></video>
      <div v-else class="xc-preview-unknown">暂不支持的预览类型</div>
    </div>
  </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator'
import Pagination from '@/components/pagination/index.vue'
import MediaAddDialog from './components/media-add-dialog.vue'
import { IKVData } from '@/api/types'
import { RESOURCE_STATUS, MEDIA_AUDIT_STATUS } from '@/api/constants'
import { IMediaPageList } from '@/entity/media-page-list'
import { getMediaPageList, previewMedia, deleteMedia } from '@/api/media'

@Component({
  components: {
    Pagination,
    MediaAddDialog
  }
})
export default class MediaList extends Vue {
  // 资料类型列表
  private mediaTypes: IKVData[] = RESOURCE_STATUS
  // 审核状态列表
  private mediaAuditStatus: IKVData[] = MEDIA_AUDIT_STATUS


  // 是否载入中
  private listLoading: boolean = false
  // 请求参数Query
  private listQuery = {
    pageNo: 1,
    pageSize: 10
  }
  // 请求参数body
  private listQueryData = {
    filename: '',
    fileType: '',
    sortBy: 'createDate',
    sortOrder: 'desc'
  }
  // 媒资列表
  private listResult: IMediaPageList = {}
  // 上传资料对话框
  private dialogVisible: boolean = false
  // 排序
  private sortProp: string = 'createDate'
  private sortOrder: 'ascending' | 'descending' | null = null
  // 预览
  private previewVisible: boolean = false
  private previewUrl: string = ''
  private previewType: 'image' | 'video' | 'file' = 'file'

  // 计算属性
  getMediaAuditStatus(auditStatus: string) {
    let item = this.mediaAuditStatus.find((value: IKVData) => {
      return auditStatus == value.code
    })
    return !item ? '' : item.desc
  }
    getMediaTypes(fileType: string) {
    let item = this.mediaTypes.find((value: IKVData) => {
      return fileType == value.code
    })
    return !item ? '' : item.desc
  }
  

  /**
   * 媒资列表
   */
  private async getMediaPageList() {
    this.listLoading = true
    this.listResult = await getMediaPageList(this.listQuery, this.listQueryData)
    this.listLoading = false
  }

  /**
   * 上传资料
   */
  private handleOpenAddMediaDialog() {
    this.dialogVisible = true
  }

  /**
   * 查看原因
   */
  private handleViewAuditMind(auditMind: string) {
    this.$message({
      showClose: true,
      message: auditMind
    })
  }

  /**
   * 下载媒资
   */
  private async handleDownloadMedia(id: string) {
    const playUrl = await previewMedia(id)
    if (!playUrl) {
      this.$message({
        type: 'error',
        message: '资源未生成预览地址，请稍后重试或联系管理员'
      })
      return
    }
    window.open(`${process.env.VUE_APP_SERVER_PICSERVER_URL}` + playUrl)
  }

  // 预览（图片/视频在当前页遮罩展示）
  private async handlePreview(row: any) {
    const id = row.fileId || row.id
    const url = await previewMedia(String(id))
    if (!url) {
      this.$message({ type: 'error', message: '资源未生成预览地址，请稍后重试或联系管理员' })
      return
    }
    const full = `${process.env.VUE_APP_SERVER_PICSERVER_URL}` + url
    const isImage = (row.fileType === '001001') || /\.(png|jpg|jpeg|gif|webp)$/i.test(full)
    const isVideo = (row.fileType === '001002') || /\.(mp4|mov|m4v|webm|ogg)$/i.test(full)
    this.previewUrl = full
    this.previewType = isImage ? 'image' : (isVideo ? 'video' : 'file')
    this.previewVisible = true
  }
    
  // private async handleDownloadMedia(id: number) {
  //   let playUrl= await previewMedia(id);
  //   if(!playUrl){
  //     this.$message({
  //     type: 'error',
  //     message: "视频没有转码处理"
  //   })
  //     return;
  //   }
  //   window.open("/api/media/test?url="+`${process.env.VUE_APP_SERVER_PICSERVER_URL}` +"/"+playUrl);
  // }

  /**
   * 移除媒资
   */
  private handleOpenDeleteMediaConfirm(id: number) {
    this.$confirm('此操作将永久移除该文件, 是否继续?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(async () => {
        let flag: boolean = await deleteMedia(id)
        this.getMediaPageList()
        this.$message({
          type: 'success',
          message: '移除成功!'
        })
      })
      .catch(() => {
        this.$message({
          type: 'info',
          message: '已取消移除'
        })
      })
  }

  // 监控 watch
  // 搜索栏
  @Watch('listQueryData', { deep: true, immediate: true })
  private watchListQueryData(newVal: string) {
    if (newVal == '') {
      return
    }
    this.getMediaPageList()
  }

  // 处理表格排序变更
  private handleSortChange({ prop, order }) {
    this.sortProp = prop
    this.sortOrder = order
    // 将三态映射到后端
    if (order === 'descending') {
      this.listQueryData.sortBy = prop
      this.listQueryData.sortOrder = 'desc'
    } else if (order === 'ascending') {
      this.listQueryData.sortBy = prop
      this.listQueryData.sortOrder = 'asc'
    } else {
      // 取消排序：恢复默认（后端默认按createDate desc）
      delete (this.listQueryData as any).sortBy
      delete (this.listQueryData as any).sortOrder
    }
    this.listQuery.pageNo = 1
    this.getMediaPageList()
  }

  // 翻页 pageSize
  @Watch('listQuery.pageSize', { immediate: true })
  private watchListQueryPageSize(newVal: number) {
    this.listQuery.pageNo = 1
  }
}
</script>

<style lang="scss" scoped>
.course-list {
  .nav-bar {
    margin-top: 16px;
  }

  .workspace .banner .btn-add {
    float: right;
  }

  .searcher {
    margin-top: 16px;

    div {
      width: 218px;
      margin-right: 10px;
    }
  }

  .dataList {
    margin-top: 16px;
  }

  .dataList-pagination {
    text-align: center;
    width: 100%;
  }
}

/* 预览遮罩层样式 */
.xc-preview-mask {
  position: fixed;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.65);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.xc-preview-box {
  max-width: 80vw;
  max-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.xc-preview-img, .xc-preview-video {
  max-width: 80vw;
  max-height: 80vh;
  object-fit: contain;
  background: #000;
}
.xc-preview-unknown {
  color: #fff;
}
</style>
