package com.dong.apiclientsdk.client;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.dong.apiclientsdk.common.ErrorCode;
import com.dong.apiclientsdk.readerStrategy.FileProcessor;
import com.dong.apiclientsdk.utils.ThrowUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;

import static com.dong.apiclientsdk.utils.SignUtils.genSign;

public class ApiClient {

    private static final String GATEWAY_HOST = "http://localhost:8099";

    private String accessKey;

    private String secretKey;

    public ApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Resource
    private FileProcessor fileProcessor;

    private Map<String, String> getHeaderMap(String body,String name, String textType) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("accessKey", accessKey);
        // 一定不能直接发送
//        hashMap.put("secretKey", secretKey);
        hashMap.put("nonce", RandomUtil.randomNumbers(4));
        hashMap.put("body", body);
        hashMap.put("name", name);
        hashMap.put("textType", textType);
        hashMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        hashMap.put("sign", genSign(body, secretKey));
        return hashMap;
    }


    public String genText(MultipartFile multipartFile, String name, String textType) {

        //校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        final long ONE_MB = 1024*1024;
        ThrowUtils.throwIf(size>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1MB");
        ThrowUtils.throwIf(size==0, ErrorCode.PARAMS_ERROR,"文件为空");
        //校验文件后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("txt","doc","docx","md");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀名非法");


        ArrayList<String> files = fileProcessor.processFile(suffix, multipartFile);
        String json = JSONUtil.toJsonStr(files);
        HttpResponse httpResponse = HttpRequest.post(GATEWAY_HOST + "/text/api/gen")
                .addHeaders(getHeaderMap(json,name,textType))
                .body(json)
                .execute();
        System.out.println(httpResponse.getStatus());
        String result = httpResponse.body();
        System.out.println(result);
        return result;

    }


}
