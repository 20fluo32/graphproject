package com.demo.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.demo.config.ODConfig;
import com.demo.domain.Detection;
import com.demo.service.VideoRecognitionService;
import com.demo.utils.Letterbox;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;

@Service
public class VideoRecognitionServiceImpl implements VideoRecognitionService {

    static {
        // 加载 OpenCV 动态库
        nu.pattern.OpenCV.loadLocally();
        System.load("E:\\graduate desiner\\graphTransferProject\\backend\\src\\main\\resources\\lib\\opencv_videoio_ffmpeg470_64.dll");
    }

    private static final float CONFIDENCE_THRESHOLD = 0.35F;
    private static final float NMS_THRESHOLD = 0.55F;

    private final String modelPath = "src\\main\\resources\\model\\yolov9c.onnx";
    private final List<double[]> colors = new ArrayList<>();
    private String[] labels;

    public String processVideo(MultipartFile file, List<Integer> limitLabels) throws IOException, OrtException {
        // 获取项目的根目录
        String projectRoot = System.getProperty("user.dir");
        // 设置上传目录为 src/main/resources/static/video
        String uploadDir = projectRoot + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "video";
        // 创建目录
        createDirectory(uploadDir);

        // 保存上传的视频文件
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new IOException("File name is null");
        }
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uploadedFileName = "uploaded_video_" + timestamp + extension;

        String videoPath = uploadDir + File.separator + uploadedFileName;
        File videoFile = new File(videoPath);
        file.transferTo(videoFile);

        // 加载ONNX模型
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();

        OrtSession session = environment.createSession(modelPath, sessionOptions);
        String meteStr = session.getMetadata().getCustomMetadata().get("names");

        labels = new String[meteStr.split(",").length];
        System.out.println("标签数量：" + labels.length);
        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(meteStr);

        int h = 0;
        while (matcher.find()) {
            labels[h] = matcher.group(1);
            Random random = new Random();
            double[] color = {random.nextDouble() * 256, random.nextDouble() * 256, random.nextDouble() * 256};
            colors.add(color);
            h++;
        }

        // 输出模型输入信息
        session.getInputInfo().keySet().forEach(x -> {
            try {
                System.out.println("input name = " + x);
                System.out.println(session.getInputInfo().get(x).getInfo().toString());
            } catch (OrtException e) {
                throw new RuntimeException(e);
            }
        });

        // 打开视频文件
        VideoCapture videoCapture = new VideoCapture(videoFile.getAbsolutePath());
        if (!videoCapture.isOpened()) {
            throw new IOException("Unable to open video file.");
        }

        // 获取视频帧大小
        int frameWidth = (int) videoCapture.get(CAP_PROP_FRAME_WIDTH);
        int frameHeight = (int) videoCapture.get(CAP_PROP_FRAME_HEIGHT);
        Size frameSize = new Size(frameWidth, frameHeight);

        // 设置输出文件路径
        String rawOutputFileName = "raw_output_video_" + timestamp + extension;
        String rawOutputPath = uploadDir + File.separator + rawOutputFileName;
        VideoWriter videoWriter = new VideoWriter(rawOutputPath, VideoWriter.fourcc('m', 'p', '4', 'v'), 6, frameSize);

        // 定义跳帧间隔
        int frameSkip = 4; // 每处理1帧，跳过3帧
        int frameCounter = 0;
        // 视频检测循环
        while (true) {
            Mat frame = new Mat();
            if (!videoCapture.read(frame)) {
                break; // 读取完所有帧时退出
            }
            frameCounter++;
            // 如果当前帧不需要处理，直接跳过
            if (frameCounter % frameSkip != 0) {
                continue;
            }
            Mat image = frame.clone();
            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);

            int minDwDh = Math.min(frame.width(), frame.height());
            int thickness = minDwDh / ODConfig.lineThicknessRatio;
            long start_time = System.currentTimeMillis();

            // 更改 frame 尺寸
            Letterbox letterbox = new Letterbox();
            image = letterbox.letterbox(image);

            double ratio = letterbox.getRatio();
            double dw = letterbox.getDw();
            double dh = letterbox.getDh();
            int rows = letterbox.getHeight();
            int cols = letterbox.getWidth();
            int channels = image.channels();

            // 将Mat对象的像素值赋值给Float[]对象
            float[] pixels = new float[channels * rows * cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    double[] pixel = image.get(j, i);
                    for (int k = 0; k < channels; k++) {
                        pixels[rows * cols * k + j * cols + i] = (float) pixel[k] / 255.0f;
                    }
                }
            }

            // 创建OnnxTensor对象
            long[] shape = {1L, (long) channels, (long) rows, (long) cols};
            OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(pixels), shape);
            HashMap<String, OnnxTensor> stringOnnxTensorHashMap = new HashMap<>();
            stringOnnxTensorHashMap.put(session.getInputInfo().keySet().iterator().next(), tensor);

            // 运行推理
            OrtSession.Result output = session.run(stringOnnxTensorHashMap);
            float[][] outputData = ((float[][][]) output.get(0).getValue())[0];

            outputData = transposeMatrix(outputData);
            Map<Integer, List<float[]>> class2Bbox = new HashMap<>();

            for (float[] bbox : outputData) {
                float[] conditionalProbabilities = Arrays.copyOfRange(bbox, 4, bbox.length);
                int label = argmax(conditionalProbabilities);
                float conf = conditionalProbabilities[label];
                if (conf < CONFIDENCE_THRESHOLD) continue;

                bbox[4] = conf;

                // xywh to (x1, y1, x2, y2)
                xywh2xyxy(bbox);

                if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) continue;

                class2Bbox.putIfAbsent(label, new ArrayList<>());
                class2Bbox.get(label).add(bbox);
            }

            List<Detection> detections = new ArrayList<>();
            for (Map.Entry<Integer, List<float[]>> entry : class2Bbox.entrySet()) {
                int label = entry.getKey();
                // 如果 limitLabels 不为空且当前标签不在 limitLabels 中，则跳过
                if (limitLabels != null && limitLabels.size() > 0 && !limitLabels.contains(label)) {
                    continue;
                }
                List<float[]> bboxes = entry.getValue();
                bboxes = nonMaxSuppression(bboxes, NMS_THRESHOLD);
                for (float[] bbox : bboxes) {
                    String labelString = labels[label];
                    detections.add(new Detection(labelString, entry.getKey(), Arrays.copyOfRange(bbox, 0, 4), bbox[4]));
                }
            }

            for (Detection detection : detections) {
                float[] bbox = detection.getBbox();
                System.out.println(detection.toString());
                Point topLeft = new Point((bbox[0] - dw) / ratio, (bbox[1] - dh) / ratio);
                Point bottomRight = new Point((bbox[2] - dw) / ratio, (bbox[3] - dh) / ratio);
                Scalar color = new Scalar(colors.get(detection.getClsId()));
                Imgproc.rectangle(frame, topLeft, bottomRight, color, thickness);

                // 框上写文字
                Point boxNameLoc = new Point((bbox[0] - dw) / ratio, (bbox[1] - dh) / ratio - 3);
                Imgproc.putText(frame, detection.getLabel(), boxNameLoc, Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, color, thickness);
            }

            System.out.printf("time：%d ms.\n", (System.currentTimeMillis() - start_time));


            // 保存视频帧
            videoWriter.write(frame);
        }

        videoCapture.release();
        videoWriter.release();
        // 视频处理完成后，转换为H264格式
        String finalOutputFileName = "output_video_" + timestamp + ".mp4";
        String finalOutputPath = uploadDir + File.separator + finalOutputFileName;
        convertVideoToH264(rawOutputPath, finalOutputPath);

        System.out.println("视频处理完成，输出文件路径：" + finalOutputPath);
        return finalOutputFileName; // 返回最终的视频文件名字
    }
    private void convertVideoToH264(String inputVideoPath, String outputVideoPath) throws IOException {
        String ffmpegPath = "D:\\Program Files\\ffmpeg\\ffmpeg-2025-02-02-git-957eb2323a-full_build\\bin\\ffmpeg.exe";
        // FFmpeg命令将视频转换为H264编码
        String ffmpegCommand = String.format("%s -i \"%s\" -c:v libx264 -crf 23 -preset fast \"%s\"", ffmpegPath, inputVideoPath, outputVideoPath);

        // 执行FFmpeg命令
        Process process = new ProcessBuilder(ffmpegCommand.split(" "))
                .redirectErrorStream(true)
                .start();

        // 读取FFmpeg的输出流
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg process failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            throw new IOException("FFmpeg process interrupted", e);
        }
    }
    private void createDirectory(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + dirPath);
            }
        }
    }

    public static float[][] transposeMatrix(float[][] m) {
        float[][] temp = new float[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }


    private int argmax(float[] scores) {
        int index = 0;
        float max = scores[0];
        for (int i = 1; i < scores.length; i++) {
            if (scores[i] > max) {
                max = scores[i];
                index = i;
            }
        }
        return index;
    }

    private void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];
        bbox[0] = x - w / 2;
        bbox[1] = y - h / 2;
        bbox[2] = x + w / 2;
        bbox[3] = y + h / 2;
    }

    /**
     * 实现非极大值抑制（NMS）
     *
     * @param bboxes       检测框的列表，每个框格式：[x1, y1, x2, y2, confidence]
     * @param iouThreshold IoU阈值，用于过滤重叠度较大的框（通常为0.3~0.5）
     * @return 过滤后的检测框列表
     */
    public static List<float[]> nonMaxSuppression(List<float[]> bboxes, float iouThreshold) {

        List<float[]> bestBboxes = new ArrayList<>();

        bboxes.sort(Comparator.comparing(a -> a[4]));

        while (!bboxes.isEmpty()) {
            float[] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < iouThreshold).collect(Collectors.toList());
        }

        return bestBboxes;
    }

    /**
     * 计算两个框的交并比（IoU）
     *
     * @param box1 第一个框，格式：[x1, y1, x2, y2, confidence]
     * @param box2 第二个框，格式：[x1, y1, x2, y2, confidence]
     * @return 两个框的IoU值
     */
    public static float computeIOU(float[] box1, float[] box2) {
        // 计算第一个框的面积
        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        // 计算第二个框的面积
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        // 计算交集框的坐标
        float interLeft = Math.max(box1[0], box2[0]);
        float interTop = Math.max(box1[1], box2[1]);
        float interRight = Math.min(box1[2], box2[2]);
        float interBottom = Math.min(box1[3], box2[3]);

        // 计算交集框的面积
        float interArea = Math.max(interRight - interLeft, 0) * Math.max(interBottom - interTop, 0);

        // 计算并集框的面积
        float unionArea = area1 + area2 - interArea;

        // 返回IoU（交集面积/并集面积）
        return interArea / unionArea;
    }

}
