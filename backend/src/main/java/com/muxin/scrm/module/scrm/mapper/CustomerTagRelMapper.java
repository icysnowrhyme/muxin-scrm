package com.muxin.scrm.module.scrm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muxin.scrm.module.scrm.model.bo.TagCountBO;
import com.muxin.scrm.module.scrm.model.entity.CustomerTagRel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomerTagRelMapper extends BaseMapper<CustomerTagRel> {

    @Select("SELECT tag_id AS tagId, COUNT(*) AS count FROM rel_customer_tag GROUP BY tag_id")
    List<TagCountBO> countByTag();
}
