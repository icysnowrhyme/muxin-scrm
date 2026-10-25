package com.muxin.scrm.module.codegen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muxin.scrm.module.codegen.mapper.GenTableColumnMapper;
import com.muxin.scrm.module.codegen.model.entity.GenTableColumn;
import com.muxin.scrm.module.codegen.service.GenTableColumnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 代码生成字段配置服务实现类
 *
 * @author MUXIN Team
 * @since 2.10.0
 */
@Service
@RequiredArgsConstructor
public class GenTableColumnServiceImpl extends ServiceImpl<GenTableColumnMapper, GenTableColumn> implements GenTableColumnService {


}
