package com.xuecheng.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程分类服务实现类
 * <p>
 * 核心功能：将Mapper查询的扁平数据组装成树形结构
 */
@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

    @Autowired
    CourseCategoryMapper courseCategoryMapper;

    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {

        // ========== 第1步：调用Mapper递归查询所有分类节点 ==========
        // 这里查询出来的是扁平的List，包含所有层级的节点（包括根节点）
        List<CourseCategoryTreeDto> categoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        // ========== 第2步：将List转换为Map，方便后续快速查找节点 ==========
        // 为什么要转Map？因为通过Map的key（节点ID）查找节点的时间复杂度是O(1)
        // 如果用List查找，需要遍历，时间复杂度是O(n)

        // 同时在这一步过滤掉根节点（根节点不需要返回给前端）
        Map<String, CourseCategoryTreeDto> mapTemp = categoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId())) // 排除根节点
                .collect(Collectors.toMap(
                        key -> key.getId(), // Map的key：节点ID
                        value -> value, // Map的value：节点对象本身
                        (key1, key2) -> key2 // key冲突时的处理策略（保留后者）
                ));

        // ========== 第3步：定义最终返回的List（只包含一级分类） ==========
        List<CourseCategoryTreeDto> categoryList = new ArrayList<>();

        // ========== 第4步：遍历所有节点，组装树形结构 ==========
        // 核心算法：
        // 1. 如果节点是一级分类（parent_id等于传入的id），放入返回List
        // 2. 对于每个节点，找到它的父节点，将自己添加到父节点的childrenTreeNodes中

        categoryTreeDtos.stream()
                .filter(item -> !id.equals(item.getId())) // 排除根节点
                .forEach(item -> {

                    // 4.1 判断当前节点是否为一级分类
                    // 一级分类的特征：它的parent_id等于传入的根节点id
                    if (item.getParentid().equals(id)) {
                        categoryList.add(item); // 一级分类放入最终返回的List
                    }

                    // 4.2 从Map中查找当前节点的父节点
                    // 为什么要找父节点？因为要把当前节点放入父节点的childrenTreeNodes中
                    CourseCategoryTreeDto parentNode = mapTemp.get(item.getParentid());

                    // 4.3 如果父节点存在（不为空）
                    // 注意：根节点不在Map中（已被过滤），所以一级分类的父节点会是null
                    if (parentNode != null) {

                        // 4.3.1 检查父节点的子节点列表是否已初始化
                        // 如果为空，需要先初始化一个ArrayList
                        if (parentNode.getChildrenTreeNodes() == null) {
                            parentNode.setChildrenTreeNodes(new ArrayList<>());
                        }

                        // 4.3.2 将当前节点添加到父节点的子节点列表中
                        // 这一步是关键：建立父子关系
                        parentNode.getChildrenTreeNodes().add(item);
                    }
                });

        // ========== 第5步：返回组装好的树形结构数据 ==========
        // 返回的List中只包含一级分类
        // 但每个一级分类的childrenTreeNodes中包含了它的所有子分类
        return categoryList;
    }
}
