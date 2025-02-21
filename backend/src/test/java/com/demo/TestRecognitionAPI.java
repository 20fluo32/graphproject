package com.demo;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestRecognitionAPI {
    public static void main(String[] args) throws Exception {
        String url = "http://localhost:8080/video/recognize"; // 替换成你的接口URL
        File videoFile = new File("D:\\github\\yolo-onnx-java\\video\\car3.mp4"); // 你的视频文件
        JSONArray labels = new JSONArray();
        labels.put(1); // 添加标签 1: 人
        labels.put(2); // 添加标签 2: 车

        // 创建连接
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=--boundary");

        // 发送请求体数据
        try (OutputStream outputStream = connection.getOutputStream()) {
            String boundary = "--boundary";
            outputStream.write(("--boundary\r\n").getBytes());
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + videoFile.getName() + "\"\r\n").getBytes());
            outputStream.write(("Content-Type: video/mp4\r\n\r\n").getBytes());
            try (FileInputStream fileInputStream = new FileInputStream(videoFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            outputStream.write(("\r\n--boundary\r\n").getBytes());
            outputStream.write(("Content-Disposition: form-data; name=\"labels\"\r\n\r\n").getBytes());
            outputStream.write((labels + "\r\n").getBytes());
            outputStream.write(("--boundary--\r\n").getBytes());
            outputStream.flush();
        }

        // 读取接口返回结果
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            System.out.println("Response: " + connection.getResponseMessage());
            // 如果返回的是处理后的视频流，应该有一个流处理的逻辑
            // 或者返回一个URL，指向处理后的视频
        } else {
            System.out.println("Error: " + connection.getResponseCode() + " " + connection.getResponseMessage());
        }
    }
}
