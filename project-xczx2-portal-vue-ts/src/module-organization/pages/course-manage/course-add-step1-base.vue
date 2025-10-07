<template>
  <div class="step-body">
    <el-form
      ref="form"
      v-if="baseInfoData"
      :model="baseInfoData"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="课程名称：" prop="name">
        <el-input v-model="baseInfoData.name"></el-input>
      </el-form-item>
      <el-form-item label="课程标签：" prop="tags">
        <el-input v-model="baseInfoData.tags"></el-input>
      </el-form-item>
      <el-form-item label="课程分类：" prop="uiCategoryTreeSelected">
        <el-cascader
          v-if="categoryTreeData.length > 0"
          v-model="baseInfoData.uiCategoryTreeSelected"
          :options="categoryTreeData"
          :props="defaultProps"
          @change="handleCategoryTreeChange"
          clearable
          placeholder="请选择课程分类"
        ></el-cascader>
        <span v-else style="color: #999">课程分类加载中...</span>
      </el-form-item>
      <el-form-item label="课程等级：" prop="grade">
        <el-select v-model="baseInfoData.grade" placeholder="请选择">
          <el-option
            v-for="item in gradeData"
            :key="item.code"
            :label="item.desc"
            :value="item.code"
          ></el-option>
        </el-select> 
      </el-form-item>
      <el-form-item label="课程简介：">
        <el-input v-model="baseInfoData.description" type="textarea" :rows="5"></el-input>
      </el-form-item>
      <!--<el-form-item label="课程目标：">
        <el-input v-model="baseInfoData.objectives" type="textarea" :rows="5"></el-input>
      </el-form-item>-->
      <el-form-item label="适用人群：" prop="users">
        <el-input v-model="baseInfoData.users" type="textarea" :rows="5"></el-input>
      </el-form-item>
      <el-form-item label="课程封面：" prop="pic">
        <common-entering-step2-upload-image 
          :imageUrl.sync="baseInfoData.pic"
          @upload-success="handleImageUploadSuccess">
          图片要求 点击查看课程封面模版规范
          <br />尺寸大于1080*608 分辨率小于96dpi
        </common-entering-step2-upload-image>
        <!-- <el-input v-model="baseInfoData.pic"></el-input> -->
      </el-form-item>
      <el-form-item label="课程类型：" prop="charge">
        <template v-for="item in chargeData">
          <el-radio :key="item.code" v-model="baseInfoData.charge" :label="item.code">{{item.desc}}</el-radio>
        </template>
      </el-form-item>
      <el-form-item label="原价：" prop="originalPrice">
        <el-input v-model="baseInfoData.originalPrice" style="width:150px;"></el-input>
        <!-- <span>&nbsp;元</span> -->
      </el-form-item>
       <el-form-item label="现价：" prop="price">
        <el-input v-model="baseInfoData.price" style="width:150px;"></el-input>
        <!-- <span>&nbsp;元</span> -->
      </el-form-item>
      <el-form-item label="咨询QQ：" prop="qq">
        <el-input v-model="baseInfoData.qq" style="width:150px;"></el-input>
      </el-form-item>
      <el-form-item label="微信号：" prop="wechat">
        <el-input v-model="baseInfoData.wechat" style="width:150px;"></el-input>
      </el-form-item>
      <el-form-item label="电话：" prop="phone">
        <el-input v-model="baseInfoData.phone" style="width:150px;"></el-input>
      </el-form-item>
            <el-form-item label="有效期(天)：" prop="validDays">
        <el-input v-model="baseInfoData.validDays" style="width:150px;"></el-input>
      </el-form-item>
    </el-form>
  </div>
</template>


<script lang="ts">
import { Component, Prop, PropSync, Watch, Vue } from 'vue-property-decorator'
import { IKVData } from '@/api/types'
import { COUSE_GRADE_STATUS, COURSE_CHARGE_TYPE_STATUS } from '@/api/constants'
import { category, submitBaseInfo, getBaseInfo } from '@/api/courses'
import { ICourseCategory, ICourseBaseInfo } from '@/entity/course-add-base'
import { ElForm } from 'element-ui/types/form'
import CommonEnteringStep2UploadImage from '@/module-entering/pages/entering/components/common-entering-step2-upload-image.vue'

@Component({
  name: 'CourseAddStep1BaseInfo',
  components: {
    CommonEnteringStep2UploadImage
  }
})
export default class extends Vue {
  @PropSync('courseBaseId', {})
  syncCourseBaseId!: number

  @Prop({ type: String })
  teachmode!: string

  private baseInfoData!: ICourseBaseInfo // 课程基本信息
  private categoryTreeData: ICourseCategory[] = [] // 课程分类
  private defaultProps = {
    children: 'childrenTreeNodes',
    value: 'id',
    label: 'label'
  }
  // private categoryTreeSelected: string[] = ['', ''] // 被选中的项目
  private gradeData: IKVData[] = COUSE_GRADE_STATUS // 课程等级
  private chargeData: IKVData[] = COURSE_CHARGE_TYPE_STATUS // 课程类型 收费 免费

  constructor() {
    super()
    this.baseInfoData = {
      charge: '201000',
      price: 0,
      qq:'',
      wechat:'',
      phone:'',
      validDays:365,
      mt: '',
      st: '',
      name: '',
      pic: '',
      teachmode: this.teachmode,
      users: '',

      tags: '',
      grade: '',
      objectives: '',

      uiCategoryTreeSelected: []
    }
  }

  // 读取数据
  public async getData() {
    // alert(this.baseInfoData.pic)
    if (this.syncCourseBaseId != 0) {
      console.log('📥 开始加载课程详情，courseId:', this.syncCourseBaseId)
      
      let data = await getBaseInfo(this.syncCourseBaseId)
      console.log('📦 服务端返回数据:', data)
      console.log('📦 mt值:', data.mt, 'st值:', data.st)
      
      // 重要：先设置分类选中值，再赋值给baseInfoData
      const categorySelected = [data.mt, data.st]
      console.log('🎯 转换后的分类数组:', categorySelected)
      
      // 使用Vue.$set确保uiCategoryTreeSelected是响应式的
      this.$set(data, 'uiCategoryTreeSelected', categorySelected)
      
      // 赋值给表单数据
      this.baseInfoData = data
      
      // 处理图片URL：只有当pic是相对路径时才拼接服务器地址
      if (this.baseInfoData.pic && !this.baseInfoData.pic.startsWith('http')) {
        this.baseInfoData.pic = `${process.env.VUE_APP_SERVER_PICSERVER_URL}`+this.baseInfoData.pic
      }
      console.log('📷 图片URL:', this.baseInfoData.pic)
      
      // 手动触发视图更新
      this.$nextTick(() => {
        console.log('✅ 视图更新完成')
        console.log('✅ baseInfoData.mt:', this.baseInfoData.mt)
        console.log('✅ baseInfoData.st:', this.baseInfoData.st)
        console.log('✅ baseInfoData.uiCategoryTreeSelected:', this.baseInfoData.uiCategoryTreeSelected)
        console.log('✅ categoryTreeData长度:', this.categoryTreeData.length)
      })
    }
  }

  // 保存数据
  public async saveData(): Promise<boolean> {

    return new Promise(async (resolve, reject) => {
      let valid: boolean = await this.validateForm()
      if (valid) {
            //图片路径去掉网址
        this.baseInfoData.pic = this.baseInfoData.pic.replace(`${process.env.VUE_APP_SERVER_PICSERVER_URL}`,'')
               const result: ICourseBaseInfo = await submitBaseInfo(this.baseInfoData)
        if (result.id !== undefined) {
          this.syncCourseBaseId = result.id
          resolve()
        }
      } else {
        this.$message.error('请正确输入表单内容')
        reject()
      }
    })
  }

  // 验证规则
  private rules = {
    name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
    uiCategoryTreeSelected: [
      {
        required: true,
        message: '请选择课程分类',
        trigger: 'change'
      }
    ],
    charge: [{ required: true, message: '请输入收费规则', trigger: 'blur' }],
    grade: [{ required: true, message: '请输入课程等级', trigger: 'blur' }],
    users: [{ required: true, message: '请输入适用人群', trigger: 'blur' }]
    // price: [
    //   {
    //     required: true,
    //     message: '请正确输入课程价格',
    //     trigger: 'change'
    //   }
    // ]
  }
  public validateForm(): Promise<boolean> {
    return new Promise(resolve => {
      let form: ElForm = this.$refs['form'] as ElForm
      form.validate(valid => resolve(valid))
    })
  }

  async created() {
    // 第1步：加载课程分类树数据（必须先加载，供级联选择器使用）
    this.categoryTreeData = await category()
    console.log('✅ 课程分类树加载完成，数据条数:', this.categoryTreeData ? this.categoryTreeData.length : 0)
    
    // 第2步：如果是编辑模式（有courseBaseId），加载课程详情
    if (this.syncCourseBaseId && this.syncCourseBaseId != 0) {
      await this.getData()
      console.log('✅ 课程详情加载完成，courseId:', this.syncCourseBaseId)
    } else {
      console.log('ℹ️ 新增模式，无需加载课程详情')
    }
  }

  // 事件 handle
  private handleCategoryTreeChange(data) {
    console.log(data)
    this.baseInfoData.mt = data[0]
    this.baseInfoData.st = data[1]
  }

  /**
   * 🔥 核心功能：处理图片上传成功事件，自动保存课程信息
   * 
   * 工作流程：
   * 1. 上传组件触发upload-success事件
   * 2. 此方法接收完整图片URL
   * 3. 立即调用保存接口，更新course_base表的pic字段
   * 4. 无需点击"保存并下一步"按钮
   */
  private async handleImageUploadSuccess(payload: { fullUrl: string; objectName: string }) {
    const { fullUrl, objectName } = payload || { fullUrl: '', objectName: '' }
    console.log('🖼️ 图片上传成功回调触发')
    console.log('🖼️ 图片URL:', fullUrl)
    console.log('🖼️ 对象名:', objectName)
    console.log('🖼️ 当前courseBaseId:', this.syncCourseBaseId)

    // 只在编辑模式（已有courseBaseId）时自动保存
    if (this.syncCourseBaseId && this.syncCourseBaseId !== 0) {
      try {
        // 数据库仅存储对象名（相对路径）
        const picPath = objectName || fullUrl.replace(`${process.env.VUE_APP_SERVER_PICSERVER_URL || ''}`, '')

        console.log('💾 开始自动保存课程图片...')
        console.log('💾 相对路径:', picPath)
        console.log('💾 完整课程数据:', this.baseInfoData)

        // 构建保存数据（避免直接修改baseInfoData）
        const dataToSave = {
          ...this.baseInfoData,
          pic: picPath
        }

        // 调用保存接口（PUT请求，更新课程）
        const result: ICourseBaseInfo = await submitBaseInfo(dataToSave)

        if (result.id !== undefined) {
          this.$message.success('课程封面已自动保存！')
          console.log('✅ 课程图片自动保存成功！')
          console.log('✅ 返回结果:', result)
        }
      } catch (error: any) {
        console.error('❌ 自动保存课程图片失败:', error)

        // 详细的错误信息
        if (error.response) {
          console.error('❌ 错误响应:', error.response.data)
          const errMsg = error.response.data.message || error.response.data.errMessage || '保存失败'
          this.$message.error(`自动保存失败：${errMsg}`)
        } else {
          this.$message.error('自动保存失败，请检查网络连接')
        }
      }
    } else {
      console.log('ℹ️ 新增模式（无courseBaseId），暂不自动保存')
      this.$message.warning('请先填写课程基本信息并保存，创建课程后才能自动保存图片')
    }
  }

  // 监控 watch
  // 注意：图片上传的自动保存逻辑已移到 handleImageUploadSuccess 方法中
  // 不再使用 Watch 监听器，避免复杂的判断逻辑和潜在的bug
  // 搜索栏
  // @Watch('baseInfoData', { deep: true, immediate: true })
  // private watchListQueryData(newVal: ICourseBaseInfo) {
  //   if (newVal === undefined) {
  //     return
  //   }
  //   this.categoryTreeSelected = [newVal.mt, newVal.st]
  // }
}
</script>

<style lang="scss" scoped>
.step-body {
  width: 800px;
  margin: 0px auto;
}
</style>