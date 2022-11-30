package com.zhang.crm.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhang.crm.base.BaseService;
import com.zhang.crm.dao.CustomerMapper;
import com.zhang.crm.dao.CustomerServeMapper;
import com.zhang.crm.dao.UserMapper;
import com.zhang.crm.enums.CustomerServeStatus;
import com.zhang.crm.query.CustomerServeQuery;
import com.zhang.crm.utils.AssertUtil;
import com.zhang.crm.vo.CustomerServe;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomerServeService extends BaseService<CustomerServe, Integer> {
    @Autowired
    private CustomerServeMapper customerServeMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private UserMapper userMapper;


    public Map<String, Object> queryCustomerServeByParams(CustomerServeQuery customerServeQuery) {
        Map<String, Object> map = new HashMap<>();

        //开启分页
        PageHelper.startPage(customerServeQuery.getPage(), customerServeQuery.getLimit());
        //得到分页对象(对CusDevPlanQuery对象进行分页）
        PageInfo<CustomerServe> pageInfo = new PageInfo<>(customerServeMapper.selectByParams(customerServeQuery));
        map.put("code", 0);
        map.put("msg", "success");
        map.put("count", pageInfo.getTotal());
        //设置分页好的列表
        map.put("data", pageInfo.getList());
        return map;
    }

    /**
     * 添加服务操作
     * 1. 参数校验
     * 客户名 customer
     * 非空，客户表中存在客户记录
     * 服务类型 serveType
     * 非空
     * 服务请求内容  serviceRequest
     * 非空
     * 2. 设置参数的默认值
     * 服务状态
     * 服务创建状态  fw_001
     * 是否有效
     * 创建时间
     * 更新时间
     * 创建人 createPeople
     * （前端页面中通过cookie获取，传递到后台）
     * 2. 执行添加操作，判断受影响的行数
     *
     * @param customerServe
     * @return void
     */
    @Transactional
    public void add(CustomerServe customerServe) {
        checkParams(customerServe);
        //设置参数的默认值
        customerServe.setServeType(CustomerServeStatus.CREATED.getState());
        customerServe.setIsValid(1);
        customerServe.setCreateDate(new Date());
        customerServe.setUpdateDate(new Date());
        //执行添加操作
        AssertUtil.isTrue(customerServeMapper.insertSelective(customerServe) < 1, "服务添加操作失败");
    }

    /**
     * 1. 参数校验
     * 客户名 customer
     * 非空，客户表中存在客户记录
     * 服务类型 serveType
     * 非空
     * 服务请求内容  serviceRequest
     * 非空
     *
     * @param customerServe
     */
    private void checkParams(CustomerServe customerServe) {
        AssertUtil.isTrue(customerServe.getCustomer() == null, "客户名称不能为空");
        AssertUtil.isTrue(customerMapper.selectByCustomerByName(customerServe.getCustomer()) == null, "客户记录不存在");
        AssertUtil.isTrue(StringUtils.isBlank(customerServe.getServeType()), "客户服务类型不能为空");
        AssertUtil.isTrue(customerServe.getServiceRequest() == null, "客户请求内容不能为空");
    }

    /**
     * 服务分配/服务处理/服务反馈
     * 1. 参数校验与设置参数的默认值
     * 客户服务ID
     * 非空，记录必须存在
     * 客户服务状态
     * 如果服务状态为 服务分配状态 fw_002
     * 分配人
     * 非空，分配用户记录存在
     * 分配时间
     * 系统当前时间
     * 更新时间
     * 系统当前时间
     * <p>
     * 如果服务状态为 服务处理状态 fw_003
     * 服务处理内容
     * 非空
     * 服务处理时间
     * 系统当前时间
     * 更新时间
     * 系统当前时间
     * <p>
     * 如果服务状态是 服务反馈状态  fw_004
     * 服务反馈内容
     * 非空
     * 服务满意度
     * 非空
     * 更新时间
     * 系统当前时间
     * 服务状态
     * 设置为 服务归档状态 fw_005
     * <p>
     * 2. 执行更新操作，判断受影响的行数
     *
     * @param customerServe
     * @return void
     */
    @Transactional
    public void update(CustomerServe customerServe) {
        // 客户服务ID  非空且记录存在
        AssertUtil.isTrue(customerServe.getId() == null
                || customerServeMapper.selectByPrimaryKey(customerServe.getId()) == null, "待更新的服务记录不存在！");

        // 判断客户服务的服务状态
        if (CustomerServeStatus.ASSIGNED.getState().equals(customerServe.getState())) {
            // 服务分配操作
            // 分配人       非空，分配用户记录存在
            AssertUtil.isTrue(StringUtils.isBlank(customerServe.getAssigner()), "待分配用户不能为空！");
            AssertUtil.isTrue(userMapper.selectByPrimaryKey(Integer.parseInt(customerServe.getAssigner())) == null, "待分配用户不存在！");
            // 分配时间     系统当前时间
            customerServe.setAssignTime(new Date());


        } else if (CustomerServeStatus.PROCED.getState().equals(customerServe.getState())) {
            // 服务处理操作
            // 服务处理内容   非空
            AssertUtil.isTrue(StringUtils.isBlank(customerServe.getServiceProce()), "服务处理内容不能为空！");
            // 服务处理时间   系统当前时间
            customerServe.setServiceProceTime(new Date());

        } else if (CustomerServeStatus.FEED_BACK.getState().equals(customerServe.getState())) {
            // 服务反馈操作
            // 服务反馈内容   非空
            AssertUtil.isTrue(StringUtils.isBlank(customerServe.getServiceProceResult()), "服务反馈内容不能为空！");
            // 服务满意度     非空
            AssertUtil.isTrue(StringUtils.isBlank(customerServe.getMyd()), "请选择服务反馈满意度！");
            // 服务状态      设置为 服务归档状态 fw_005
            customerServe.setState(CustomerServeStatus.ARCHIVED.getState());
        }

        // 更新时间     系统当前时间
        customerServe.setUpdateDate(new Date());

        // 执行更新操作，判断受影响的行数
        AssertUtil.isTrue(customerServeMapper.updateByPrimaryKeySelective(customerServe) < 1, "服务更新失败！");

    }
}
