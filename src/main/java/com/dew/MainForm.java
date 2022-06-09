/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dew;

import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 *
 * @author Vũ Gia Long - B18DCAT154
 */
@SuppressWarnings("serial")
public class MainForm extends javax.swing.JFrame {

    /**
     * Creates new form MainForm
     */
    private String message;
    private JLabel label1, label2;
    private int DD;
    private int n, step, videoTotalFrame;
    private DCT dct;
    private int videoHeight, videoWidth;
    private BufferedImage frame, frameDecode;
    private String filePath1, filePath2, filePositionKey;
    private String positionKey, videoType;
    private DEW dew;
    private boolean isRetry;

    public MainForm() {
        initComponents();
        setTitle("Difference Energy Watermarking - Nhóm 17");
        label1 = new JLabel();
        label2 = new JLabel();
        jTextField1.setEditable(false);
        jTextField4.setEditable(false);
//        jPanel4.setLayout(new GridBagLayout());
        jPanel5.setLayout(new GridBagLayout());
        jPanel6.setLayout(new GridBagLayout());
        dct = new DCT(0);
        DD = 50;
        jTextField6.setText("70");
        jTextField7.setText("0");
        step = 0;
        positionKey = "";
    }

    private int[] binaryTranform() {
        message = jTextField3.getText();
        
        byte[] bin = message.getBytes();
        String tmp = "";
        for (int i = 0; i < bin.length; i++) {
        	int val = bin[i] & 0xff;
            String temp = "";
            while (val > 0) {
                if (val % 2 == 0) {
                    temp += 0;
                    
                } else {
                    temp += 1;
                }
                val /= 2;
            }
            while (temp.length() < 8) {
                temp += "0";
            }
            tmp += new StringBuilder(temp).reverse().toString();
        }
        String binaryText = "";
        int[] binaryMessage = new int[tmp.length()];
        for (int i = 0; i < tmp.length(); i++) {
            binaryMessage[i] = Integer.parseInt(tmp.charAt(i) + "");
            if(i%8 == 0) binaryText +=" "+Integer.parseInt(tmp.charAt(i) + "");
            else binaryText +=Integer.parseInt(tmp.charAt(i) + "");
        }

        jEditorPane2.setText(binaryText.trim());
        return binaryMessage;
    }

    private ArrayList<ArrayList<int[][]>> getBlockListNxN(ArrayList<int[][]> blockDCT, BufferedImage frame) {
        ArrayList<ArrayList<int[][]>> res = new ArrayList<>();
        int frameWidth = frame.getWidth() / 8;
        int frameHeight = frame.getHeight()/ 8;
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
        System.out.println("Số lượng khối lc: "+ res.size());
        return res;
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

                sumA += blockDCTA[row][col] * blockDCTA[row][col];
                sumB += blockDCTB[row][col] * blockDCTB[row][col];

                if (sumA >= DD && i > maxEA) {
                    maxEA = i;
                }
                if (sumB >= DD && i > maxEB) {
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
                sumB += blockDCTB[row][col] * blockDCTB[row][col];
                sumA += blockDCTA[row][col] * blockDCTA[row][col];
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

    private void reEncodeVideo(String filePath, BufferedImage frame) {
    	ProgressBarPanel progress = new ProgressBarPanel();
        Java2DFrameConverter c = new Java2DFrameConverter();
        Frame encodeFrame = c.getFrame(frame);
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        FFmpegFrameRecorder frameRecorder = new FFmpegFrameRecorder(new File("E:\\Nam 4\\KTGT\\videomahoa."+videoType), videoWidth, videoHeight);
        try {
        	progress.showFrame();
        	
        	frameGrabber.setFrameNumber(0);
            frameGrabber.start();
            frameRecorder.setAudioChannels(frameGrabber.getAudioChannels());
            frameRecorder.setAudioBitrate(frameGrabber.getAudioBitrate());
            frameRecorder.setFrameRate(frameGrabber.getFrameRate());
            frameRecorder.setVideoBitrate(frameGrabber.getVideoBitrate());
            frameRecorder.start();
            int frameNumber = 0;
            Frame frameGrab;
            while ((frameGrab= frameGrabber.grabFrame()) != null) {
            	
                if (frameNumber == 122) {
                    frameRecorder.record(encodeFrame);
                }
                else { 
                    frameRecorder.record(frameGrab);
                }
	            if(frameGrab.image != null) {    
	                frameNumber++;
	                System.out.println(frameNumber*100/videoTotalFrame +"%");
	                progress.setValue(frameNumber*100/videoTotalFrame);
            	}
            }
            System.out.println((frameNumber+2)*100/videoTotalFrame +"%");
            progress.setValue((frameNumber+2)*100/videoTotalFrame);
            frameGrabber.stop();
            frameRecorder.stop();
            frameRecorder.release();
            frameGrabber.close();
            c.close();
            JOptionPane.showMessageDialog(this, "Hoàn thành");
            System.out.println("Done");
            writeFileKey(dew.getPositionKey());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void redecode(BufferedImage frame) {
        ArrayList<BufferedImage> block8x8List = new ArrayList<>();
        for (int x = 0; x < frame.getHeight(); x = x + 8) {
            for (int y = 0; y < frame.getWidth(); y = y + 8) {
                block8x8List.add(frame.getSubimage(y, x, 8, 8));
            }
        }
        ArrayList<YCrCb[][]> blockYCrCb = new ArrayList<>();
        ArrayList<int[][]> blockDCT = new ArrayList<>();
        for (int i = 0; i < block8x8List.size(); i++) {

            YCrCb[][] yCCmatrix = new YCrCb[8][8];
            int[][] matrix = new int[8][8];
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    Color color = new Color(block8x8List.get(i).getRGB(y, x));
                    int red = color.getRed();
                    int green = color.getGreen();
                    int blue = color.getBlue();
                    YCrCb yCrCb = new YCrCb(red, green, blue);
                    yCCmatrix[y][x] = yCrCb;
                    matrix[y][x] = yCrCb.getY();
                }
            }
            int matrixTest[][] = dct.quantitizeImage(dct.forwardDCT(matrix), true);
            blockYCrCb.add(yCCmatrix);
            blockDCT.add(matrixTest);
        }
        ArrayList<ArrayList<int[][]>> blockNxNList = getBlockListNxN(blockDCT, frame);
        String m = "";
        String[] positionInFrame = positionKey.trim().split(" ");
        for (int i = 0; i < positionInFrame.length; i++) {
            int t = extractBit(blockNxNList.get(Integer.parseInt(positionInFrame[i])));
            if (t == -1) {
                break;
            }
            if(i % 8 == 0 && i> 0) m += " ";
            m += t;
            
        }
        System.out.println(m);
        jEditorPane1.setText(m);
        String binArr[] = m.trim().split(" ");
        byte[] bval = new byte[binArr.length];
        for (int i = 0; i < bval.length; i++) {
        	
        	int val = Integer.parseInt(binArr[i], 2);
        	System.out.println(val);
            bval[i] = (byte) val;
            
        }

        try {
            jTextField9.setText(new String(bval, "UTF-8"));
            System.out.println(new String(bval, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MainForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private BufferedImage getFrameToEncode(String filePath) {
        
        FFmpegFrameGrabber frameGrabber = new FFmpegFrameGrabber(filePath);
        try {
            frameGrabber.start();
            videoTotalFrame = frameGrabber.getLengthInVideoFrames();
            int audioLength =frameGrabber.getLengthInAudioFrames();
            System.out.println("Tổng số frame: "+ videoTotalFrame+ ", audioLength: " + audioLength);
            System.out.println("Bitrate: " + frameGrabber.getVideoBitrate());
            
            Java2DFrameConverter c = new Java2DFrameConverter();
            int frameNumber = 0;
            Frame fTemp;
            BufferedImage tmp = null;
            while((fTemp = frameGrabber.grabImage()) != null) {
            	if(frameNumber == 122) {
            		tmp = c.getBufferedImage(fTemp);
            		break;
            	}
            	frameNumber++;
            }
            
             
            videoHeight = tmp.getHeight();
            videoWidth = tmp.getWidth();
            label1.setIcon(new ImageIcon(new ImageIcon(tmp).getImage().getScaledInstance(jPanel5.getWidth(), jPanel5.getHeight(), Image.SCALE_DEFAULT)));
            jPanel5.add(label1);
            c.close();
            frameGrabber.stop();
            frameGrabber.close();
            return tmp;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
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
                if (frameNumber == 122) {
                    frame = c.getBufferedImage(bTmp);
                    break;
                }
                frameNumber++;
            }
            
            videoHeight = frame.getHeight();
            videoWidth = frame.getWidth();
            c.close();
            frameGrabber.stop();
            frameGrabber.close();
            return frame;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    private void showImage(ArrayList<ArrayList<int[][]>> blockNxNList, ArrayList<YCrCb[][]> blockYCrCb, BufferedImage frame, int n) {
      // Convert to image and show image -----------------------------------------------
      int r = 0;
      int row = 0;
      int frameWidth = frame.getWidth() / 8;
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
	                          blockYCrCb.get(indexOfYCrCb)[x][y].setY((int) block.get(ind)[x][y]);
	                          frame.setRGB(i + x, j + y, blockYCrCb.get(indexOfYCrCb)[x][y].getColorFromYCbCr().getRGB());
                          } else {
                        	  frame.setRGB(i + x, j + y, block.get(ind)[x][y]);
                          }

                      }
                  }
                  ind++;
              }
          }
      }
      if(step == 3) {
    	  this.frame = frame;
      }
      label2.setIcon(new ImageIcon(new ImageIcon(frame).getImage().getScaledInstance(jPanel6.getWidth(), jPanel6.getHeight(), Image.SCALE_DEFAULT)));
      jPanel6.add(label2);
    }
    
    private void readFileKey(String filePositionKey) {
		try {
			FileReader fis = new FileReader(filePositionKey);
			BufferedReader ois = new BufferedReader(fis);
			positionKey = ois.readLine();
			System.out.println("Position key: " + positionKey);
			ois.close();
			fis.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
    
    private void writeFileKey(String key) {
    	try {
			FileWriter fileWriter = new FileWriter("E:\\Nam 4\\KTGT\\key.txt");
			fileWriter.write(key);
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

    	jPanel1 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jEditorPane2 = new javax.swing.JEditorPane();
        jLabel10 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        jTextField7 = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jTextField5 = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jEditorPane1 = new javax.swing.JEditorPane();
        jLabel8 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jTextField8 = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jTextField9 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jTextField10 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jButton1.setText("Chọn file...");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel1.setText("File:");

        jLabel2.setText("Kích thước vùng lc:");

        jLabel3.setText("Thông điệp:");

        jLabel4.setText("Khung hình ngẫu nhiên được lấy:");

        jButton2.setText("Tiếp theo");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel5.setText("Khung hình sau khi được mã hóa:");

        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel5.setPreferredSize(new java.awt.Dimension(480, 262));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 480, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel5, java.awt.BorderLayout.WEST);

        jPanel6.setPreferredSize(new java.awt.Dimension(480, 262));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 480, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel6, java.awt.BorderLayout.EAST);

        jLabel9.setText("<html>Thông điệp dạng nhị phân:</html>");

        jScrollPane2.setViewportView(jEditorPane2);

        jLabel10.setText("Khác biệt năng lượng D:");

        jLabel11.setText("Chỉ số cmin:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, 968, Short.MAX_VALUE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(305, 305, 305)
                                .addComponent(jLabel5)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel1)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jButton1))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel10)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel2)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGap(18, 18, 18)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addGap(18, 18, 18)
                                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel11)
                                                .addGap(18, 18, 18)
                                                .addComponent(jTextField7)))))
                                .addGap(18, 18, 18)
                                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 108, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jButton1)
                                .addComponent(jLabel1)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(10, 10, 10)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel2)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel10)
                                .addComponent(jLabel11)
                                .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Mã hóa", jPanel2);

        jLabel6.setText("File video:");

        jButton3.setText("Chọn file...");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel7.setText("Kích thước vùng lc:");

        jScrollPane1.setViewportView(jEditorPane1);

        jLabel8.setText("Thông điệp nhị phân lấy được:");

        jButton4.setText("Giải mã");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel12.setText("Khác biệt năng lượng D':");

        jLabel13.setText("Thông điệp lấy được:");

        jLabel14.setText("File vị trí:");

        jButton5.setText("Chọn file...");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(215, 215, 215)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField4)
                        .addGap(18, 18, 18)
                        .addComponent(jButton3))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField10)
                        .addGap(18, 18, 18)
                        .addComponent(jButton5))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField9))
                    .addComponent(jLabel8)
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 161, Short.MAX_VALUE)
                        .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(215, 215, 215))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jLabel6)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(2, 2, 2)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(jLabel14)
                    .addComponent(jTextField10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(jLabel12)
                    .addComponent(jTextField8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jTextField9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 218, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(100, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Giải mã", jPanel3);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>                        

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        JFileChooser jFileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MPEG video", "mpeg");
        jFileChooser.setFileFilter(filter);
        int returnVal = jFileChooser.showDialog(jPanel1, "Chọn video");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String videoName = jFileChooser.getSelectedFile().getName();
            videoType = videoName.substring(videoName.indexOf(".")+1, videoName.length());
            filePath1 = jFileChooser.getSelectedFile().getAbsolutePath();
            jTextField1.setText(filePath1);
            System.out.println("Video type: "+ videoType);
            step = 0;
            isRetry = false;
            videoTotalFrame = 0;
        }
        
    }                                        

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
    	switch(step) {
	    	case 0:{
	            try {
	            	int[] binaryMessage = binaryTranform();
	                int n = Integer.parseInt(jTextField2.getText());
	                int D = Integer.parseInt(jTextField6.getText());
	                int minC = Integer.parseInt(jTextField7.getText());
	                BufferedImage frame = getFrameToEncode(filePath1);
	                dew = new DEW(D, n ,minC, binaryMessage, frame);
	            } catch (NumberFormatException ex) {
	                JOptionPane.showMessageDialog(rootPane, "kích thước n sai định dạng");
	            }
	            break;
	    	}
	    	case 1:{
	    		dew.calBlockNxNlist();
	    		BufferedImage frame = new BufferedImage(dew.getFrame().getWidth(), dew.getFrame().getHeight(), dew.getFrame().getType());
	    		showImage(dew.getBlockNxNList(), null, frame, dew.getN());
	    		jLabel5.setText("Biến đổi DCT:");
	    		break;
	    	}
	    	case 2: {
	    		try {
	    			if(isRetry) {
	    				int[] binaryMessage = binaryTranform();
		    			int n = Integer.parseInt(jTextField2.getText());
		                int D = Integer.parseInt(jTextField6.getText());
		                int minC = Integer.parseInt(jTextField7.getText());
		                dew.reSetupArgs(D, n, minC, binaryMessage);
		                isRetry = false;
	    			}
	    			dew.execEmbeddedMessage();
	    			BufferedImage frame = new BufferedImage(dew.getFrame().getWidth(), dew.getFrame().getHeight(), dew.getFrame().getType());
		    		showImage(dew.getBlockNxNList(), null, frame, dew.getN());
		    		jLabel5.setText("Các khối DCT sau khi nhúng thông điệp:");
	    			break;
	    		} catch (Exception e) {
	    			JOptionPane.showMessageDialog(rootPane, e.getMessage());
	    			isRetry = true;
	    			return;
				}
	    	}
	    	case 3: {
	    		dew.inverstDCT();
	    		showImage(dew.getBlockNxNList(), dew.getBlockYCrCb(), dew.getFrame(), dew.getN());
	    		jLabel5.setText("Khung hình được phục hồi sau khi nhúng thông điệp:");
	    		break;
	    	}
	    	case 4:{
	    		Runnable runner = new Runnable()
	            {
	                public void run() {
	                	reEncodeVideo(jTextField1.getText(),frame);
	                }
	            };
	            Thread t = new Thread(runner);
	            t.start();
	    		break;
	    	}
	    		
    	}
    	step++;
    	if(step == 5)
    		step = 0;
    	
    }                                        

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        JFileChooser jFileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("MPEG video", "mpeg");
        jFileChooser.setFileFilter(filter);
        frame = null;
        int returnVal = jFileChooser.showDialog(jPanel1, "Chọn video");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePath2 = jFileChooser.getSelectedFile().getAbsolutePath();
            jTextField4.setText(filePath2);
        }
    }                                        

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {                                         
        try {
            n = Integer.parseInt(jTextField5.getText());
            DD = Integer.parseInt(jTextField8.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Sai định dạng");
            return;
        }
        readFileKey(filePositionKey);
        if(positionKey.equals("")) {
        	JOptionPane.showMessageDialog(this, "Key không có");
        	return;
        }
        frameDecode = getFrameToDecode(filePath2);
        redecode(frameDecode);
    }                                        
    
    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {                                         
    	JFileChooser jFileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("File *txt", "txt");
        jFileChooser.setFileFilter(filter);
        int returnVal = jFileChooser.showDialog(this, "Chọn file");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            filePositionKey = jFileChooser.getSelectedFile().getAbsolutePath();
            jTextField10.setText(filePositionKey);
            
        }
    }  
    

	/**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new MainForm().setVisible(true);
            }
        });

    }

    // Variables declaration - do not modify                     
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JEditorPane jEditorPane2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTextField jTextField8;
    private javax.swing.JTextField jTextField9;
    private javax.swing.JTextField jTextField10;
    // End of variables declaration                   
}
