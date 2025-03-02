package com.demo.controller;

import ai.onnxruntime.OrtException;
import com.demo.service.impl.ImageTransferServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/image")
public class ImageTransferController {

    @Autowired
    private ImageTransferServiceImpl imageTransferServiceImpl;

    @PostMapping("/process")
    public String processImage(@RequestParam("file") MultipartFile file) throws IOException, OrtException {
        return imageTransferServiceImpl.processImage(file);
    }
}