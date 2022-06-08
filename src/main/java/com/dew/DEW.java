package com.dew;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class DEW {
	private final DCT dct = new DCT(0);
	private BufferedImage frame;
	private int D, n, minC;
	private int[] binaryMessage;
	private ArrayList<ArrayList<int[][]>> blockNxNList;
	private ArrayList<YCrCb[][]> blockYCrCb;
    private String postionKey;
    
    public DEW(int D, int n, int minC, int[] binaryMessage, BufferedImage frame) {
    	this.D = D;
    	this.n = n;
    	this.minC = minC;
    	this.binaryMessage = binaryMessage;
    	this.frame = frame;
    	blockYCrCb = new ArrayList<>();
    	postionKey = "";
    }
    
    public void reSetupArgs(int D, int n, int minC, int[] binaryMessage) {
    	this.D = D;
    	this.minC = minC;
    	this.binaryMessage = binaryMessage;
    	if(this.n != n) {
    		this.n = n;
	    	blockYCrCb = new ArrayList<>();
	    	calBlockNxNlist();
    	}
    }
    
    public void execEmbeddedMessage() throws Exception {
    	// Find index cut-off C in frame -----------------------------------------------------
    	postionKey = "";
        int k = 0;
        for (int i = 0; i < blockNxNList.size(); i++) {
            if(k == binaryMessage.length) break;
            Map<Integer, Integer> mapIndexC = getIndexCutOffC(blockNxNList.get(i));
            if(mapIndexC == null || mapIndexC.isEmpty()) continue;
            blockNxNList.set(i, embedMessage(blockNxNList.get(i), k, mapIndexC, i));
            k++;
            postionKey += i+" ";
        }
        System.out.println(postionKey);
        if(postionKey.trim().split(" ").length != binaryMessage.length){
            throw new Exception("Không đủ vị trí để nhúng");

        }
    }
    
    public void inverstDCT() {
        // Inverst DCT -----------------------------------------------------------------------
        for (int i = 0; i < blockNxNList.size(); i++) {
            ArrayList<int[][]> blockLC = blockNxNList.get(i);
            for (int j = 0; j < blockLC.size(); j++) {
                blockLC.set(j, dct.inverseDCT(dct.dequantitizeImage(blockLC.get(j), true)));
            }
            blockNxNList.set(i, blockLC);
        }
    }
    
    public void calBlockNxNlist() {
    	// Divide frame to 8x8 block pixel ----------------------------------------------------
        ArrayList<BufferedImage> block8x8List = new ArrayList<>();
        for (int x = 0; x < frame.getHeight(); x = x + 8) {
            for (int y = 0; y < frame.getWidth(); y = y + 8) {
                block8x8List.add(frame.getSubimage(y, x, 8, 8));
            }
        }
        
        // DCT each block pixel --------------------------------------------------------------
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
        
        // Relocate all block to ArrayList ---------------------------------------------------
        blockNxNList = getBlockListNxN(blockDCT, frame);
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
        System.out.println(res.size());
        return res;
    }
	
	private Map<Integer, Integer> getIndexCutOffC(ArrayList<int[][]> blockLC) {
        
        Map<Integer, Integer> mapIndexOffC = new HashMap<>();
        for (int index = 0; index < n * n / 2; index++) {
            int res = -1;
            int[][] blockDCTA = blockLC.get(index);
            int[][] blockDCTB = blockLC.get(n * n / 2 + index);
            int sumA = 0;
            int sumB = 0;
            int[][] zigZag = dct.zigZag;
            int row, col;
            for (int i = 64 - 1; i >= minC; i--) {
                row = zigZag[i][0];
                col = zigZag[i][1];

                sumA += blockDCTA[row][col] * blockDCTA[row][col];
                sumB += blockDCTB[row][col] * blockDCTB[row][col];

                if (sumA >= D && sumB >= D) {
                    res = Math.max(i, res);
                }
            }
            if(res == -1) return null;
            mapIndexOffC.put(index, res);
        }
        
        return mapIndexOffC;
    }
	
	private ArrayList<int[][]> embedMessage(ArrayList<int[][]> blockLC, int i, Map<Integer, Integer> mapIndexC, int indexBlock) {
        int label = binaryMessage[i];
        System.out.println("K nhung thu: "+ i+", Khoi thu: "+ indexBlock);
        for (int index = 0; index < n * n / 2; index++) {
            int c = mapIndexC.get(index);
            int[][] blockDCTB = blockLC.get(n * n / 2 + index);
            int[][] blockDCTA = blockLC.get(index);
            int[][] zigZag = dct.zigZag;
            int row, col;
            for (int j = 63; j >= c; j--) {
                row = zigZag[j][0];
                col = zigZag[j][1];
                if(label == 0) blockDCTB[row][col] = 0;
                else blockDCTA[row][col] = 0;
            }
            if(label == 0) blockLC.set(n * n / 2 + index, blockDCTB);
            else blockLC.set(index, blockDCTA);
        }
        return blockLC;
    }
    
    public ArrayList<ArrayList<int[][]>> getBlockNxNList(){
    	return this.blockNxNList;
    }
    
    public BufferedImage getFrame() {
    	return this.frame;
    }
    
    public ArrayList<YCrCb[][]> getBlockYCrCb() {
		return blockYCrCb;
	}
    
    public int getN() {
		return n;
	}
	
    public String getPostionKey() {
    	return postionKey;
    }
	
}
