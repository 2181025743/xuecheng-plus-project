package com.xuecheng.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XuechengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams,
            QueryCourseParamsDto queryCourseParamsDto) {
        // 2. 构建查询条件
        log.info("2. 构建分页查询条件");
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();

        // 课程名称模糊查询
        queryWrapper.like(!StringUtils.isEmpty(queryCourseParamsDto.getCourseName()),
                CourseBase::getName,
                queryCourseParamsDto.getCourseName());

        // 课程审核状态精确查询
        queryWrapper.eq(!StringUtils.isEmpty(queryCourseParamsDto.getAuditStatus()),
                CourseBase::getAuditStatus,
                queryCourseParamsDto.getAuditStatus());

        // 课程发布状态精确查询
        queryWrapper.eq(!StringUtils.isEmpty(queryCourseParamsDto.getPublishStatus()),
                CourseBase::getStatus,
                queryCourseParamsDto.getPublishStatus());

        // 3. 设置分页参数
        log.info("分页参数：第{}页，每页{}条", pageParams.getPageNo(), pageParams.getPageSize());
        log.info("查询条件：课程名称={}，审核状态={}，发布状态={}",
                queryCourseParamsDto.getCourseName(),
                queryCourseParamsDto.getAuditStatus(),
                queryCourseParamsDto.getPublishStatus());

        // 4. 执行分页查询
        log.info("3. 执行分页查询...");
        Page<CourseBase> courseBasePage = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        Page<CourseBase> result = courseBaseMapper.selectPage(courseBasePage, queryWrapper);

        // 5. 封装结果
        List<CourseBase> items = result.getRecords();
        long total = result.getTotal();
        PageResult<CourseBase> courseBasePageResult = new PageResult<>(
                items, total, pageParams.getPageNo(), pageParams.getPageSize());

        // 6. 验证结果
        log.info("========== 查询结果 ==========");
        log.info("✅ 总记录数：{}", total);
        log.info("✅ 当前页码：{}", courseBasePageResult.getPage());
        log.info("✅ 每页大小：{}", courseBasePageResult.getPageSize());
        log.info("✅ 本页数据：{} 条", items.size());

        items.forEach(item -> {
            log.info("  - 课程ID：{}，课程名称：{}，审核状态：{}，发布状态：{}",
                    item.getId(), item.getName(), item.getAuditStatus(), item.getStatus());
        });

        log.info("========== 测试完成 ==========\n");

        return courseBasePageResult;
    }

    @Override
    @Transactional // 事务注解：确保两张表同时成功或失败
    public CourseBaseInfoDto createCourse(Long companyId, AddCourseDto addCourseDto) {

        // 2.1 创建CourseBase对象
        CourseBase courseBase = new CourseBase();

        // 2.2 将DTO中的属性拷贝到CourseBase对象
        // BeanUtils.copyProperties：只要属性名相同就自动拷贝
        // 注意：如果源对象的属性为null，会覆盖目标对象的非null值
        BeanUtils.copyProperties(addCourseDto, courseBase);

        // 2.3 设置系统字段（不从前端获取，由系统自动设置）
        // 这些字段必须在BeanUtils.copyProperties之后设置
        // 避免被DTO中的null值覆盖

        courseBase.setCompanyId(companyId); // 机构ID（从登录用户获取，暂时可写死）
        courseBase.setCreateDate(LocalDateTime.now()); // 创建时间（当前时间）
        courseBase.setAuditStatus("202002"); // 审核状态：未提交
        courseBase.setStatus("203001"); // 发布状态：未发布

        // 2.4 插入数据库
        int insert = courseBaseMapper.insert(courseBase);
        if (insert <= 0) {
            XuechengPlusException.cast("添加课程失败");
        }

        // 2.5 获取插入后的课程ID（MyBatis Plus会自动回填到对象中）
        Long courseId = courseBase.getId();

        // 调试日志：检查courseId是否为null
        log.info("插入course_base后，获取到的courseId = {}", courseId);

        // 防御性编程：如果courseId为null，抛出异常
        if (courseId == null) {
            XuechengPlusException.cast("插入课程后获取ID失败，请检查数据库主键配置");
        }

        // ========== 第3步：向course_market表插入/更新数据 ==========

        // 调用保存营销信息的方法（存在则更新，不存在则添加）
        saveCourseMarket(courseId, addCourseDto);

        // ========== 第4步：查询课程完整信息并返回 ==========

        // 从数据库查询课程的完整信息（基本信息+营销信息+分类名称）
        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);

        return courseBaseInfo;
    }

    @Override
    @Transactional // 事务：确保两张表同时成功或失败
    public CourseBaseInfoDto updateCourse(Long companyId, AddCourseDto addCourseDto) {

        // ===== 第1步：参数校验 =====

        // 课程ID必须存在
        Long courseId = addCourseDto.getId();
        if (courseId == null) {
            XuechengPlusException.cast("课程ID不能为空");
        }

        // 查询课程是否存在
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            XuechengPlusException.cast("课程不存在");
        }

        // 权限校验：只能修改本机构的课程
        if (!courseBase.getCompanyId().equals(companyId)) {
            XuechengPlusException.cast("只能修改本机构的课程");
        }

        // ===== 第2步：更新course_base表 =====

        // 将DTO数据拷贝到查询出的对象
        BeanUtils.copyProperties(addCourseDto, courseBase);

        // 设置更新时间
        courseBase.setChangeDate(LocalDateTime.now());

        // 更新数据库
        int update = courseBaseMapper.updateById(courseBase);
        if (update <= 0) {
            XuechengPlusException.cast("修改课程失败");
        }

        // ===== 第3步：更新course_market表 =====

        // 调用保存营销信息的方法（存在则更新，不存在则添加）
        saveCourseMarket(courseId, addCourseDto);

        // ===== 第4步：查询并返回最新的课程信息 =====

        CourseBaseInfoDto courseBaseInfo = getCourseBaseInfo(courseId);

        return courseBaseInfo;
    }

    /**
     * 保存课程营销信息
     * 逻辑：存在则更新，不存在则添加
     *
     * @param courseId     课程ID
     * @param addCourseDto 课程DTO（包含营销信息）
     * @return 影响的行数
     */
    private int saveCourseMarket(Long courseId, AddCourseDto addCourseDto) {

        // ===== 第1步：参数校验 =====

        // 校验收费规则
        String charge = addCourseDto.getCharge();
        if (StringUtils.isEmpty(charge)) {
            XuechengPlusException.cast("收费规则不能为空");
        }

        // 如果是收费课程，价格必须填写且大于0
        if ("201001".equals(charge)) { // 201001 = 收费

            // 校验现价
            Float price = addCourseDto.getPrice();
            if (price == null || price <= 0) {
                XuechengPlusException.cast("收费课程的现价不能为空并且必须大于0");
            }

            // 校验原价
            Float originalPrice = addCourseDto.getOriginalPrice();
            if (originalPrice == null || originalPrice <= 0) {
                XuechengPlusException.cast("收费课程的原价不能为空并且必须大于0");
            }

            // 校验价格逻辑：现价不能大于原价
            if (price > originalPrice) {
                XuechengPlusException.cast("现价不能大于原价");
            }
        }

        // 对于所有课程（包括免费课程），如果填写了价格，不能为负数
        Float price = addCourseDto.getPrice();
        if (price != null && price < 0) {
            XuechengPlusException.cast("课程价格不能为负数");
        }

        Float originalPrice = addCourseDto.getOriginalPrice();
        if (originalPrice != null && originalPrice < 0) {
            XuechengPlusException.cast("课程原价不能为负数");
        }

        // ===== 第2步：构建CourseMarket对象 =====

        CourseMarket courseMarket = new CourseMarket();

        // 将DTO中的营销信息拷贝到CourseMarket对象
        BeanUtils.copyProperties(addCourseDto, courseMarket);

        // ⚠️ 重要：必须在BeanUtils.copyProperties之后设置ID
        // 原因：AddCourseDto继承自CourseBase，有id字段（值为null）
        // BeanUtils.copyProperties会把null值也拷贝过来，覆盖之前设置的值
        courseMarket.setId(courseId);

        // ===== 第3步：判断是插入还是更新 =====

        // 从数据库查询该课程的营销信息
        CourseMarket courseMarketDb = courseMarketMapper.selectById(courseId);

        int result = 0;

        if (courseMarketDb == null) {
            // 数据库中不存在 → 插入
            result = courseMarketMapper.insert(courseMarket);
            if (result <= 0) {
                XuechengPlusException.cast("保存课程营销信息失败");
            }
        } else {
            // 数据库中已存在 → 更新

            // 将新数据拷贝到查询出来的对象
            BeanUtils.copyProperties(addCourseDto, courseMarketDb);

            // 确保ID不被覆盖（防御性编程）
            courseMarketDb.setId(courseId);

            // 更新数据库
            result = courseMarketMapper.updateById(courseMarketDb);
            if (result <= 0) {
                XuechengPlusException.cast("更新课程营销信息失败");
            }
        }

        return result;
    }

    /**
     * 查询课程完整信息
     * 包括：基本信息 + 营销信息 + 分类名称
     *
     * @param courseId 课程ID
     * @return 课程完整信息
     */
    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {

        // ===== 第1步：参数校验 =====
        if (courseId == null) {
            XuechengPlusException.cast("课程ID不能为空");
        }

        // ===== 第2步：查询课程基本信息 =====
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (courseBase == null) {
            // 改进：抛出异常而不是返回null，让前端知道课程不存在
            XuechengPlusException.cast("课程不存在，课程ID：" + courseId);
        }

        // ===== 第2步：查询课程营销信息 =====
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);

        // ===== 第3步：组装CourseBaseInfoDto对象 =====
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();

        // 拷贝基本信息
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);

        // 拷贝营销信息（如果存在）
        if (courseMarket != null) {
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
        }

        // ===== 第4步：查询并设置分类名称 =====
        // 问题：数据库中只存了分类的code（如"1-1"），没有存name
        // 但接口要求返回分类名称（mtName, stName）
        // 解决：根据code从course_category表查询name

        // 查询大分类名称
        String mt = courseBase.getMt(); // 大分类code
        CourseCategory mtCategory = courseCategoryMapper.selectById(mt);
        if (mtCategory != null) {
            courseBaseInfoDto.setMtName(mtCategory.getName());
        }

        // 查询小分类名称
        String st = courseBase.getSt(); // 小分类code
        CourseCategory stCategory = courseCategoryMapper.selectById(st);
        if (stCategory != null) {
            courseBaseInfoDto.setStName(stCategory.getName());
        }

        return courseBaseInfoDto;
    }
}
