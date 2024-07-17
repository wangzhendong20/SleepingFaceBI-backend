package com.dong.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.user.api.model.entity.Orders;
import org.springframework.stereotype.Service;
import com.dong.user.mapper.OrdersMapper;
import com.dong.user.service.OrdersService;

/**
* @author dong
* @description 针对表【orders(充值订单表)】的数据库操作Service实现
* @createDate 2023-07-06 20:36:41
*/
@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders>
    implements OrdersService {

}




