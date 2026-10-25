package com.muxin.scrm.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.muxin.scrm.system.model.entity.Config;
import com.muxin.scrm.system.model.vo.ConfigVO;
import com.muxin.scrm.system.model.form.ConfigForm;
import org.mapstruct.Mapper;

/**
 * 系统配置对象转换器
 *
 * @author MUXIN Team
 * @since 2024-7-29 11:42:49
 */
@Mapper(componentModel = "spring")
public interface ConfigConverter {

    Page<ConfigVO> toPageVo(Page<Config> page);

    Config toEntity(ConfigForm configForm);

    ConfigForm toForm(Config entity);
}
