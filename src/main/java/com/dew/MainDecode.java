package com.dew;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainDecode {

    private final int n;

    private final int dd;

    private int frameIndexDecode;

    private String binaryString;

    private String message;

    private String positionKey;

    private final DCT dct;

    public MainDecode(int n, int dd) {
        this.n = n;
        this.dd = dd;
        dct = new DCT(0);
    }

    public void startDecode(String filePath, String filePositionKey) {
        // Read file key
        readFileKey(filePositionKey);

        // Get embedded frame to decode
        BufferedImage frameDecode = getFrameToDecode(filePath);

        // decode
        decode(frameDecode);
    }

    private void readFileKey(String filePositionKey) {
        try {
            FileReader fis = new FileReader(filePositionKey);
            BufferedReader ois = new BufferedReader(fis);
            frameIndexDecode = Integer.parseInt(ois.readLine());
            positionKey = ois.readLine();

            System.out.println("Position key: " + positionKey);
            ois.close();
            fis.close();
        } catch (Exception e) {
            throw new IllegalArgumentException("File key ko hợp lệ");
        }

    }

    private BufferedImage getFrameToDecode(String filePath) {
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        Java2DFrameConverter c = new Java2DFrameConverter();
        try {
            frameGrabber.start();
            int frameNumber = 0;
            BufferedImage frame = null;
            Frame bTmp;
            while ((bTmp = frameGrabber.grabImage()) != null) {
                if (frameNumber == frameIndexDecode) {
                    frame = c.getBufferedImage(bTmp);
                    break;
                }
                frameNumber++;
            }
            c.close();
            frameGrabber.stop();
            frameGrabber.close();
            return frame;
        } catch (Exception ignored) {
        }
        return null;
    }

    private void decode(BufferedImage frame) {
        ArrayList<BufferedImage> block8x8List = CommonUtils.getBlock8x8Pixel(frame);
        ArrayList<int[][]> blockDCT = new ArrayList<>();
        for (BufferedImage bufferedImage : block8x8List) {
            int[][] matrix = new int[8][8];
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    Color color = new Color(bufferedImage.getRGB(x, y));
                    int red = color.getRed();
                    int green = color.getGreen();
                    int blue = color.getBlue();
                    YCrCb yCrCb = new YCrCb(red, green, blue);
                    matrix[x][y] = yCrCb.getY();
                }
            }
            int[][] matrixTest = dct.quantitizeImage(dct.forwardDCT(matrix), true);
            blockDCT.add(matrixTest);
        }
        ArrayList<ArrayList<int[][]>> blockNxNList = CommonUtils.getBlockListNxN(blockDCT, frame.getWidth(), frame.getHeight(), n);
        StringBuilder m = new StringBuilder();
        String[] positionInFrame = positionKey.trim().split(" ");
        for (int i = 0; i < positionInFrame.length; i++) {
            int t = extractBit(blockNxNList.get(Integer.parseInt(positionInFrame[i])));
            if (t == -1) {
                break;
            }
            if (i % 8 == 0 && i > 0) m.append(" ");
            m.append(t);

        }
        System.out.println(m);
        binaryString = m.toString();
        String[] binArr = m.toString().trim().split(" ");
        byte[] bval = new byte[binArr.length];
        for (int i = 0; i < bval.length; i++) {

            int val = Integer.parseInt(binArr[i], 2);
            System.out.println(val);
            bval[i] = (byte) val;

        }

        message = new String(bval, StandardCharsets.UTF_8);
        System.out.println(new String(bval, StandardCharsets.UTF_8));
    }

    private int extractBit(ArrayList<int[][]> blockLC) {
        Map<Integer, Integer> mapIndexC = new HashMap<>();
        for (int index = 0; index < n * n / 2; index++) {
            int maxEB = 0, maxEA = 0;
            int[][] blockDCTA = blockLC.get(index);
            int[][] blockDCTB = blockLC.get(n * n / 2 + index);
            int sumA = 0;
            int sumB = 0;
            int[][] zigZag = dct.zigZag;
            int row, col;
            for (int i = 63; i >= 0; i--) {
                row = zigZag[i][0];
                col = zigZag[i][1];

                sumA += Math.abs(blockDCTA[row][col]) * Math.abs(blockDCTA[row][col]);
                sumB += Math.abs(blockDCTB[row][col]) * Math.abs(blockDCTB[row][col]);

                if (sumA >= dd && i > maxEA) {
                    maxEA = i;
                }
                if (sumB >= dd && i > maxEB) {
                    maxEB = i;
                }
            }
            mapIndexC.put(index, Math.max(maxEA, maxEB));
        }

        int sumA = 0, sumB = 0;
        for (int index = 0; index < n * n / 2; index++) {
            int[][] blockDCTB = blockLC.get(n * n / 2 + index);
            int[][] blockDCTA = blockLC.get(index);
            int[][] zigZag = dct.zigZag;
            int indexC = mapIndexC.get(index);
            int row, col;
            for (int j = 63; j >= indexC; j--) {
                row = zigZag[j][0];
                col = zigZag[j][1];
                sumB += Math.abs(blockDCTB[row][col]) * Math.abs(blockDCTB[row][col]);
                sumA += Math.abs(blockDCTA[row][col]) * Math.abs(blockDCTA[row][col]);
            }
        }
        if (sumA > sumB) {
            return 0;
        }
        if (sumA < sumB) {
            return 1;
        }
        return -1;
    }

    public String getBinaryString() {
        return binaryString;
    }

    public String getMessage() {
        return message;
    }
}
