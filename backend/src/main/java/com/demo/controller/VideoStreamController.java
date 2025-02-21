package com.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.InputStreamResource;

import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
import java.util.List;

@RestController
public class VideoStreamController {

    String sourcePath = "D:\\github\\graduation_project\\demo\\src\\main\\resources\\";
    private String VIDEO_DIR = sourcePath + "static\\video\\";

    @GetMapping("/stream/{videoName}")
    public ResponseEntity<InputStreamResource> streamVideo(@PathVariable String videoName, HttpServletRequest request) throws IOException {
        File videoFile = new File(VIDEO_DIR + videoName);
        if (!videoFile.exists()) {
            return ResponseEntity.notFound().build();
        }
        System.out.println("获取视频，Video file path: " + videoFile.getAbsolutePath());
        long fileSize = videoFile.length();
        String rangeHeader = request.getHeader("Range");

        // 如果没有 Range 请求头，直接返回完整文件（200 OK）
        if (rangeHeader == null || rangeHeader.isEmpty()) {
            return createFullResponse(videoFile); // 新增方法，返回完整文件
        }

        // 解析 Range 请求头
        List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
        if (ranges.isEmpty()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }

        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(fileSize);
        long end = range.getRangeEnd(fileSize);

        // 如果范围覆盖整个文件，也返回 200 OK
        if (start == 0 && end == fileSize - 1) {
            return createFullResponse(videoFile);
        }

        // 否则返回 206 Partial Content
        return createPartialResponse(videoFile, start, end);
    }

    private ResponseEntity<InputStreamResource> createFullResponse(File videoFile) throws IOException {
        InputStream inputStream = new FileInputStream(videoFile);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Length", String.valueOf(videoFile.length()));
        headers.set("Content-Type", "video/mp4");
        headers.set("Accept-Ranges", "bytes"); // 可选，表明服务器支持范围请求
        headers.set("Access-Control-Allow-Origin", "*"); // 设置允许跨域访问

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(inputStream));
    }

    private ResponseEntity<InputStreamResource> createPartialResponse(File videoFile, long start, long end) throws IOException {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(videoFile, "r")) {
            randomAccessFile.seek(start);
            byte[] buffer = new byte[(int) (end - start + 1)];
            randomAccessFile.readFully(buffer);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
            InputStreamResource inputStreamResource = new InputStreamResource(byteArrayInputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Range", "bytes " + start + "-" + end + "/" + videoFile.length());
            headers.set("Content-Length", String.valueOf(buffer.length));
            headers.set("Content-Type", "video/mp4");
            headers.set("Accept-Ranges", "bytes");
            headers.set("Access-Control-Allow-Origin", "*"); // 设置允许跨域访问

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(inputStreamResource);
        }
    }
}

