package com.dew;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Objects;

public class CommonUtils {

    /**
     * Transform message to binary
     * @param message message
     * @return array of binary
     */
    public static int[] binaryTranform(String message) {
        byte[] bytes = message.getBytes();
        StringBuilder binaryStr = new StringBuilder();
        for (byte b : bytes) {
            int val = b & 0xff;
            StringBuilder temp = new StringBuilder();
            while (val > 0) {
                if (val % 2 == 0) {
                    temp.append(0);

                } else {
                    temp.append(1);
                }
                val /= 2;
            }
            while (temp.length() < 8) {
                temp.append("0");
            }
            binaryStr.append(new StringBuilder(temp.toString()).reverse());
        }

        // Convert binary string to binary array
        // "01010101" => [0,1,0,1,0,1,0,1]
        int[] binaryArr = new int[binaryStr.length()];
        for (int i = 0; i < binaryStr.length(); i++) {
            binaryArr[i] = Integer.parseInt(binaryStr.charAt(i) + "");
        }
        return binaryArr;
    }

    /**
     * Get file type from file name
     * @param fileName file name
     * @return file type
     */
    public static String getFileType(String fileName) {
        if (Objects.isNull(fileName))
            return "";

        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Get block list nxn from list block dct
     * @param blockDCT list block dct
     * @param width frame width
     * @param height frame height
     * @param n size
     * @return block list nxn
     */
    public static ArrayList<ArrayList<int[][]>> getBlockListNxN(ArrayList<int[][]> blockDCT, int width, int height, int n) {
        ArrayList<ArrayList<int[][]>> res = new ArrayList<>();
        int frameWidth = width / 8;
        int frameHeight = height/ 8;
        for (int i = 0; i < frameHeight / n; i++) {
            for (int j = 0; j < frameWidth / n; j++) {
                ArrayList<int[][]> temp = new ArrayList<>();
                for (int x = 0; x < n; x++) {
                    for (int y = i * frameWidth * n; y < i * frameWidth * n + n; y++) {

                        temp.add(blockDCT.get(y + x * frameWidth + j * n));
                    }
                }
                res.add(temp);
            }
        }
        return res;
    }

    /**
     * Get list block 8x8 pixel
     * @param frame frame
     * @return list block 8x8 pixel
     */
    public static ArrayList<BufferedImage> getBlock8x8Pixel(BufferedImage frame) {
        ArrayList<BufferedImage> block8x8List = new ArrayList<>();
        for (int y = 0; y < frame.getHeight(); y = y + 8) {
            for (int x = 0; x < frame.getWidth(); x = x + 8) {
                block8x8List.add(frame.getSubimage(x, y, 8, 8));
            }
        }
        return block8x8List;
    }
}
