package com.muxin.scrm.system.converter;

import com.muxin.scrm.system.model.entity.Dept;
import com.muxin.scrm.system.model.vo.DeptVO;
import com.muxin.scrm.system.model.form.DeptForm;
import org.mapstruct.Mapper;

/**
 * 部门对象转换器
 *
 * @author MUXIN Team
 * @since 2022/7/29
 */
@Mapper(componentModel = "spring")
public interface DeptConverter {

    DeptForm toForm(Dept entity);
    
    DeptVO toVo(Dept entity);

    Dept toEntity(DeptForm deptForm);

}