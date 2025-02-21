package com.demo.controller;

import ai.onnxruntime.OrtException;
import com.demo.service.impl.VideoRecognitionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/video")
public class VideoRecognitionController {

    @Autowired
    private VideoRecognitionServiceImpl videoRecognitionServiceImpl;

    @PostMapping("/recognize")
    public String recognizeVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "labels", required = false) List<Integer> labels) throws IOException, OrtException {

        // 如果没有传递 labels 参数，使用空列表
        if (labels == null) {
            labels = new ArrayList<>();
        }

        return videoRecognitionServiceImpl.processVideo(file, labels);
    }

}
