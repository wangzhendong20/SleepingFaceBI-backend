package com.dong.user.controller;

import cn.hutool.json.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.dong.common.common.ErrorCode;
import com.dong.common.constant.MqConstant;
import com.dong.common.excption.ThrowUtils;
import com.dong.common.mq.config.MqMessageProducer;
import com.dong.user.api.constant.OrdersConstant;
import com.dong.user.api.model.dto.order.OrderAddRequest;
import com.dong.user.api.model.entity.Orders;
import com.dong.user.api.model.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dong.user.config.AliPayConfig;
import com.dong.user.service.CreditService;
import com.dong.user.service.OrdersService;
import com.dong.user.service.UserService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付接口
 */
@RestController
@RequestMapping("/alipay")
public class AliPayController {
    private static final String GATEWAY_URL = "https://openapi.alipaydev.com/gateway.do";
    private static final String FORMAT = "JSON";
    private static final String CHARSET = "UTF-8";
    //签名方式
    private static final String SIGN_TYPE = "RSA2";
    @Resource
    private AlipayClient alipayClient;

    @Resource
    private AliPayConfig aliPayConfig;

    @Resource
    private OrdersService ordersService;

    @Resource
    private CreditService creditService;

    @Resource
    private UserService userService;

    @Resource
    private MqMessageProducer mqMessageProducer;


    @GetMapping("/pay") // &subject=xxx&traceNo=xxx&totalAmount=xxx
    public void pay(OrderAddRequest aliPay, HttpServletResponse httpResponse) throws Exception {
        //1. 插入数据库订单消息
        User loginUser = userService.getLoginUser();

        Orders orders = new Orders();
        orders.setSubject(aliPay.getSubject());
        orders.setTotalAmount(aliPay.getTotalAmount());
        orders.setUserId(loginUser.getId());
        boolean result = ordersService.save(orders);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR,"订单保存错误");

        // 2. 创建 Request并设置Request参数
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();  // 发送请求的 Request类

        request.setNotifyUrl(OrdersConstant.NOTIFYURL);
        JSONObject bizContent = new JSONObject();
        bizContent.set("out_trade_no", orders.getId());  // 我们自己生成的订单编号
        bizContent.set("total_amount", orders.getTotalAmount()); // 订单的总金额
        bizContent.set("subject", orders.getSubject());   // 支付的名称
        bizContent.set("product_code", "FAST_INSTANT_TRADE_PAY");  // 固定配置
        request.setBizContent(bizContent.toString());

        // 执行请求，拿到响应的结果，返回给浏览器
        String form = "";
        try {
            form = alipayClient.pageExecute(request).getBody(); // 调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        mqMessageProducer.sendMessage(MqConstant.ORDERS_EXCHANGE_NAME,MqConstant.ORDERS_ROUTING_KEY,String.valueOf(orders.getId()));
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form);// 直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    @PostMapping("/notify")  // 注意这里必须是POST接口
    public String payNotify(HttpServletRequest request) throws Exception {
        if (request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            System.out.println("=========支付宝异步回调========");

            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
                // System.out.println(name + " = " + request.getParameter(name));
            }

            String outTradeNo = params.get("out_trade_no");
            String buyerId = params.get("buyer_id");
            String alipayTradeNo = params.get("trade_no");
            //给金额转型
            String[] total_amounts = params.get("total_amount").split("\\.");
            Integer totalAmount = Integer.valueOf(total_amounts[0]);

            String sign = params.get("sign");
            String content = AlipaySignature.getSignCheckContentV1(params);
            boolean checkSignature = AlipaySignature.rsa256CheckContent(content, sign, aliPayConfig.getAlipayPublicKey(), "UTF-8"); // 验证签名
            // 支付宝验签
            if (checkSignature) {
//                // 验签通过
//                System.out.println("交易名称: " + params.get("subject"));
//                System.out.println("交易状态: " + params.get("trade_status"));
//                System.out.println("支付宝交易凭证号: " + params.get("trade_no"));
//                System.out.println("商户订单号: " + params.get("out_trade_no"));
//                System.out.println("交易金额: " + params.get("total_amount"));
//                System.out.println("买家在支付宝唯一id: " + params.get("buyer_id"));
//                System.out.println("买家付款时间: " + params.get("gmt_payment"));
//                System.out.println("买家付款金额: " + params.get("buyer_pay_amount"));

                // 查询订单并更新状态并加积分
                //查询当前订单消息
                Orders orders = ordersService.getById(outTradeNo);
                orders.setTradeStatus(OrdersConstant.SUCCEED);
                orders.setAlipayTradeNo(alipayTradeNo);
                orders.setBuyerId(buyerId);
                boolean result = ordersService.updateById(orders);
                ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR,"订单更新错误");
                //根据充值金额增加积分
                result = creditService.updateCredits(orders.getUserId(), 100L * totalAmount);
                ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR,"积分更新错误");
            }
        }
        return "success";
    }
}
