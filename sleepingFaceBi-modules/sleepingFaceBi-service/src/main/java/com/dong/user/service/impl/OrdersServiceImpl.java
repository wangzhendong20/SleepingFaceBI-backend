package com.dong.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dong.user.api.model.entity.Orders;
import org.springframework.stereotype.Service;
import com.dong.user.mapper.OrdersMapper;
import com.dong.user.service.OrdersService;


@Service
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders>
    implements OrdersService {

}




