package com.demo.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.demo.service.ImageTransferService;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ImageTransferServiceImpl implements ImageTransferService {

    static {
        // 加载 OpenCV 动态库
        nu.pattern.OpenCV.loadLocally();
    }

    private final String styleTransferModelPath = "E:\\graduate desiner\\graphTransferProject\\backend\\src\\main\\resources\\model\\generator_v2.onnx"; // 图像迁移模型路径

    @Override
    public String processImage(MultipartFile file) throws IOException, OrtException {
        // 获取项目的根目录
        String projectRoot = System.getProperty("user.dir");
        // 设置上传目录为 src/main/resources/static/images
        String uploadDir = projectRoot + File.separator + "backend" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "images";
        // 创建目录
        createDirectory(uploadDir);

        // 保存上传的图片文件
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new IOException("File name is null");
        }
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uploadedFileName = "uploaded_image_" + timestamp + extension;

        String imagePath = uploadDir + File.separator + uploadedFileName;
        File imageFile = new File(imagePath);
        file.transferTo(imageFile);

        // 加载图像迁移模型
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        OrtSession styleTransferSession = environment.createSession(styleTransferModelPath, sessionOptions);

        // 读取原始图片并记录尺寸
        Mat originalImage = Imgcodecs.imread(imagePath);
        if (originalImage.empty()) {
            throw new IOException("Unable to read image file.");
        }
        int originalWidth = originalImage.cols();
        int originalHeight = originalImage.rows();

        // 将图片转换为 RGB 格式
        //Imgproc.cvtColor(originalImage, originalImage, Imgproc.COLOR_BGR2RGB);

        // 调整图片尺寸为模型输入尺寸（假设模型输入为 256x256）
        int targetWidth = 256;
        int targetHeight = 256;
        Mat resizedImage = new Mat();
        Imgproc.resize(originalImage, resizedImage, new Size(targetWidth, targetHeight));

        // 将图片数据转换为浮点数组并归一化到 [-1, 1]
        float[] pixels = new float[targetWidth * targetHeight * 3];
        for (int i = 0; i < targetHeight; i++) {
            for (int j = 0; j < targetWidth; j++) {
                double[] pixel = resizedImage.get(i, j);
                for (int k = 0; k < 3; k++) {
                    pixels[k * targetHeight * targetWidth + i * targetWidth + j] = (float) ((pixel[k] / 255.0) * 2 - 1); // 归一化到 [-1, 1]
                }
            }
        }

        // 打印输入数据的最小值和最大值
        float minInputValue = Float.MAX_VALUE;
        float maxInputValue = Float.MIN_VALUE;
        for (float value : pixels) {
            if (value < minInputValue) minInputValue = value;
            if (value > maxInputValue) maxInputValue = value;
        }
        System.out.println("Input min value: " + minInputValue);
        System.out.println("Input max value: " + maxInputValue);

        // 创建 OnnxTensor 对象
        long[] shape = {1L, 3L, (long) targetHeight, (long) targetWidth};
        OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(pixels), shape);
        Map<String, OnnxTensor> inputMap = new HashMap<>();
        inputMap.put(styleTransferSession.getInputInfo().keySet().iterator().next(), tensor);

        // 运行推理
        OrtSession.Result output = styleTransferSession.run(inputMap);
        float[][][][] outputData = (float[][][][]) output.get(0).getValue();

        // 打印输出数据的最小值和最大值
        float minOutputValue = Float.MAX_VALUE;
        float maxOutputValue = Float.MIN_VALUE;
        for (int i = 0; i < targetHeight; i++) {
            for (int j = 0; j < targetWidth; j++) {
                for (int k = 0; k < 3; k++) {
                    float value = outputData[0][k][i][j];
                    if (value < minOutputValue) minOutputValue = value;
                    if (value > maxOutputValue) maxOutputValue = value;
                }
            }
        }
        System.out.println("Output min value: " + minOutputValue);
        System.out.println("Output max value: " + maxOutputValue);

        // 将输出数据从 [-1, 1] 反归一化到 [0, 1]
        Mat outputImage = new Mat(targetHeight, targetWidth, CvType.CV_32FC3);
        for (int i = 0; i < targetHeight; i++) {
            for (int j = 0; j < targetWidth; j++) {
                float[] pixel = new float[3];
                for (int k = 0; k < 3; k++) {
                    pixel[k] = (outputData[0][k][i][j] + 1) / 2.0f; // 反归一化到 [0, 1]
                }
                outputImage.put(i, j, pixel);
            }
        }

        // 将输出图片调整回原始尺寸
        Mat finalOutputImage = new Mat();
        Imgproc.resize(outputImage, finalOutputImage, new Size(originalWidth, originalHeight));

        // 将输出图片转换为 8 位无符号整数并缩放到 [0, 255]
        Mat finalOutputImage8U = new Mat();
        finalOutputImage.convertTo(finalOutputImage8U, CvType.CV_8UC3, 255.0);

        // 将输出图片保存到文件
        String outputFileName = "output_image_" + timestamp + extension;
        String outputPath = uploadDir + File.separator + outputFileName;
        Imgcodecs.imwrite(outputPath, finalOutputImage8U);

        System.out.println("图片处理完成，输出文件路径：" + outputPath);
        return outputFileName; // 返回处理后的图片文件名
    }

    private void createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + dirPath);
            }
        }
    }
}