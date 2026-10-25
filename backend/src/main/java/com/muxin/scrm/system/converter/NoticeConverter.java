package com.muxin.scrm.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muxin.scrm.system.model.bo.NoticeBO;
import com.muxin.scrm.system.model.entity.Notice;
import com.muxin.scrm.system.model.form.NoticeForm;
import com.muxin.scrm.system.model.vo.NoticeDetailVO;
import com.muxin.scrm.system.model.vo.NoticePageVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * 通知公告对象转换器
 *
 * @author MUXIN Team
 * @since 2024-08-27 10:31
 */
@Mapper(componentModel = "spring")
public interface NoticeConverter{


    @Mappings({
            @Mapping(target = "targetUserIds", expression = "java(cn.hutool.core.util.StrUtil.split(entity.getTargetUserIds(),\",\"))")
    })
    NoticeForm toForm(Notice entity);

    @Mappings({
            @Mapping(target = "targetUserIds", expression = "java(cn.hutool.core.collection.CollUtil.join(formData.getTargetUserIds(),\",\"))")
    })
    Notice toEntity(NoticeForm formData);

    NoticePageVO toPageVo(NoticeBO bo);

    Page<NoticePageVO> toPageVo(Page<NoticeBO> noticePage);

    NoticeDetailVO toDetailVo(NoticeBO noticeBo);
}
