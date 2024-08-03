package com.dong.apiclientsdk.utils;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取文本工具类
 * txt, doc, docx, md 文件读取
 */
public class TxtUtils {
    public static void main(String[] args) {
        try {
            File file = ResourceUtils.getFile("classpath:笔记.txt");

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static ArrayList<String> readerFile(MultipartFile file) {
        ArrayList<String> list= new ArrayList<>();
        try {
            // 获取文件内容
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                // 处理文件内容，例如输出到控制台
                if (builder.length()+line.length()<920){
                    builder.append(line);
                }else {
                    //保存数据库
                    list.add(builder.toString());
                    builder.delete(0,builder.length());
                }
                //保证数据不丢失
                if (builder.length()==0){
                    builder.append(line);
                }
            }
            //数据可能不超过990
            list.add(builder.toString());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static ArrayList<String> readerDocxFile(MultipartFile file) {
        ArrayList<String> list = new ArrayList<>();
        try {
            XWPFDocument doc = new XWPFDocument(file.getInputStream());
            StringBuilder builder = new StringBuilder();

            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                String line = para.getText();
                if (builder.length() + line.length() < 920) {
                    builder.append(line);
                } else {
                    list.add(builder.toString());
                    builder.setLength(0);  // clear the builder
                    builder.append(line);
                }
            }
            if (builder.length() > 0) {
                list.add(builder.toString());
            }
            doc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    public static ArrayList<String> readerDocFile(MultipartFile file) {
        ArrayList<String> list = new ArrayList<>();
        try {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.endsWith(".docx")) {
                XWPFDocument doc = new XWPFDocument(file.getInputStream());
                StringBuilder builder = new StringBuilder();

                List<XWPFParagraph> paragraphs = doc.getParagraphs();
                for (XWPFParagraph para : paragraphs) {
                    String line = para.getText();
                    if (builder.length() + line.length() < 920) {
                        builder.append(line);
                    } else {
                        list.add(builder.toString());
                        builder.setLength(0);  // clear the builder
                        builder.append(line);
                    }
                }
                if (builder.length() > 0) {
                    list.add(builder.toString());
                }
                doc.close();
            } else if (filename != null && filename.endsWith(".doc")) {
                HWPFDocument doc = new HWPFDocument(file.getInputStream());
                WordExtractor extractor = new WordExtractor(doc);
                String[] paragraphs = extractor.getParagraphText();
                StringBuilder builder = new StringBuilder();

                for (String para : paragraphs) {
                    String line = para.trim();
                    if (builder.length() + line.length() < 920) {
                        builder.append(line);
                    } else {
                        list.add(builder.toString());
                        builder.setLength(0);  // clear the builder
                        builder.append(line);
                    }
                }
                if (builder.length() > 0) {
                    list.add(builder.toString());
                }
                extractor.close();
                doc.close();
            } else {
                throw new IllegalArgumentException("Unsupported file format");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}
