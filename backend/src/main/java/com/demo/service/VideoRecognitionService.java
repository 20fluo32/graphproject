package com.demo.service;

import ai.onnxruntime.OrtException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface VideoRecognitionService {
    String processVideo(MultipartFile file, List<Integer> limitLabels) throws IOException, OrtException;
}
