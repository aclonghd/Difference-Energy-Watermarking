package com.dew;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class MainEncode {

    private final DEW dew;

    private int randomFrameIndex;

    private int n;

    private BufferedImage frame;

    private static final String WORKSPACE_PATH = "E:\\Nam 4\\KTGT\\";

    public MainEncode(int d, int n, int cMin, int[] binaryArr, String filePath) {
        this.n = n;
        getRandomFrame(filePath);
        dew = new DEW(d, n, cMin, binaryArr, frame);
    }

    public void reSetupArgs(int d, int n, int cMin, int[] binaryArr) {
        this.n = n;
        dew.reSetupArgs(d, n, cMin, binaryArr);
    }

    public void runTransformDctStep() {
        dew.calBlockNxNlist();
    }

    public void runEmbeddedStep() throws Exception {
        dew.execEmbeddedMessage();
    }

    public void runInverseDCTStep() {
        dew.inverstDCT();
    }


    public BufferedImage getFrameFromBlockNxN() {

        ArrayList<ArrayList<int[][]>> blockNxNList = dew.getBlockNxNList();
        ArrayList<YCrCb[][]> blockYCrCb = dew.getBlockYCrCb();

        // Convert block nxn to frame
        int r = 0;
        int row = 0;
        int frameWidth = frame.getWidth() / 8;
        BufferedImage res = new BufferedImage(frame.getWidth(), frame.getHeight(), frame.getType());
        for (int index = 0; index < blockNxNList.size(); index++) {
            ArrayList<int[][]> block = blockNxNList.get(index);
            int ind = 0;

            if (index % (frameWidth / n) == 0 && index != 0) {
                r = r + 1;
                row = r * n * 8;
            }
            for (int j = row; j < row + n * 8; j += 8) {
                for (int i = (index % (frameWidth/ n)) * n * 8; i < (index % (frameWidth / n)) * n * 8 + n * 8; i += 8) {

                    int indexOfYCrCb = i / 8 + j / 8 * frameWidth;
                    for (int x = 0; x < 8; x++) {
                        for (int y = 0; y < 8; y++) {
                            if(blockYCrCb != null) {
                                blockYCrCb.get(indexOfYCrCb)[x][y].setY(block.get(ind)[x][y]);
                                res.setRGB(i + x, j + y, blockYCrCb.get(indexOfYCrCb)[x][y].getColorFromYCbCr().getRGB());
                            } else {
                                res.setRGB(i + x, j + y, block.get(ind)[x][y]);
                            }

                        }
                    }
                    ind++;
                }
            }
        }
        return res;
    }

    private void getRandomFrame(String filePath) {
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        try {
            frameGrabber.start();
            int videoTotalFrame = frameGrabber.getLengthInVideoFrames();
            int audioLength = frameGrabber.getLengthInAudioFrames();
            System.out.println("Tổng số frame: " + videoTotalFrame + ", audioLength: " + audioLength);
            System.out.println("Bitrate: " + frameGrabber.getVideoBitrate());
            this.randomFrameIndex = ThreadLocalRandom.current().nextInt(0, videoTotalFrame);
            Java2DFrameConverter c = new Java2DFrameConverter();
            int frameNumber = 0;
            Frame fTemp;
            BufferedImage tmp = null;
            while ((fTemp = frameGrabber.grabImage()) != null) {
                if (frameNumber == randomFrameIndex) {
                    tmp = c.getBufferedImage(fTemp);
                    break;
                }
                frameNumber++;
            }

            c.close();
            frameGrabber.stop();
            frameGrabber.close();
            frame = tmp;
        } catch (Exception ignored) {

        }
    }

    public void reEncodeVideo(String filePath, Component parentComponent) {
        ProgressBarPanel progress = new ProgressBarPanel();
        Java2DFrameConverter c = new Java2DFrameConverter();
        Frame encodeFrame = c.getFrame(getFrameFromBlockNxN());
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        FFmpegFrameRecorder frameRecorder =
                new FFmpegFrameRecorder(
                        new File(WORKSPACE_PATH + "videomahoa." + CommonUtils.getFileType(filePath)),
                        frame.getWidth(),
                        frame.getHeight()
                );
        try {
            progress.showFrame();

            frameGrabber.setFrameNumber(0);
            frameGrabber.start();
            int videoTotalFrame = frameGrabber.getLengthInVideoFrames();
            frameRecorder.setAudioChannels(frameGrabber.getAudioChannels());
            frameRecorder.setAudioBitrate(frameGrabber.getAudioBitrate());
            frameRecorder.setFrameRate(frameGrabber.getFrameRate());
            frameRecorder.setVideoBitrate(frameGrabber.getVideoBitrate());
            frameRecorder.start();
            int frameNumber = 0;
            Frame frameGrab;
            while ((frameGrab = frameGrabber.grabFrame()) != null) {

                if (frameNumber == randomFrameIndex) {
                    frameRecorder.record(encodeFrame);
                } else {
                    frameRecorder.record(frameGrab);
                }
                if (frameGrab.image != null) {
                    frameNumber++;
                    System.out.println(frameNumber * 100 / videoTotalFrame + "%");
                    progress.setValue(frameNumber * 100 / videoTotalFrame);
                }
            }
            System.out.println((frameNumber + 2) * 100 / videoTotalFrame + "%");
            progress.setValue((frameNumber + 2) * 100 / videoTotalFrame);
            frameGrabber.stop();
            frameRecorder.stop();
            frameRecorder.release();
            frameGrabber.close();
            c.close();
            JOptionPane.showMessageDialog(parentComponent, "Hoàn thành");
            System.out.println("Done");
            writeFileKey(dew.getPositionKey());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeFileKey(String key) throws IOException {
        FileWriter fileWriter = new FileWriter(WORKSPACE_PATH + "key.txt");
        fileWriter.write(randomFrameIndex + "\n");
        fileWriter.write(key);
        fileWriter.close();
    }

    public BufferedImage getFrame() {
        return this.frame;
    }
}
