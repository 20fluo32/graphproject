package com.demo.service;

import ai.onnxruntime.OrtException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ImageTransferService {

    String processImage(MultipartFile file) throws IOException, OrtException;
}
