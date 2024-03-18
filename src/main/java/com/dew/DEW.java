package com.dew;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class DEW {
	private final DCT dct = new DCT(0);
	private final BufferedImage frame;
	private int d, n, cMin;
	private int[] binaryArr;
	private ArrayList<ArrayList<int[][]>> blockNxNList;
	private ArrayList<YCrCb[][]> blockYCrCb;
    private String positionKey;
    
    public DEW(int D, int n, int cMin, int[] binaryArr, BufferedImage frame) {
    	this.d = D;
    	this.n = n;
    	this.cMin = cMin;
    	this.binaryArr = binaryArr;
    	this.frame = frame;
    	blockYCrCb = new ArrayList<>();
    	positionKey = "";
    }

    /**
     * re setup arguments
     * @param d different energy d
     * @param n size
     * @param cMin index (min) cut-off C
     * @param binaryArr binart array
     */
    public void reSetupArgs(int d, int n, int cMin, int[] binaryArr) {
    	this.d = d;
    	this.cMin = cMin;
    	this.binaryArr = binaryArr;
    	if(this.n != n) {
    		this.n = n;
	    	blockYCrCb = new ArrayList<>();
	    	calBlockNxNlist();
    	}
    }

    /**
     * Embedded message to frame
     * @throws Exception
     */
    public void execEmbeddedMessage() throws Exception {
    	// Find index cut-off C in frame
    	positionKey = "";
        int k = 0;
        for (int i = 0; i < blockNxNList.size(); i++) {
            if(k == binaryArr.length) break;
            Map<Integer, Integer> mapIndexC = getIndexCutOffC(blockNxNList.get(i));
            if(mapIndexC == null || mapIndexC.isEmpty()) continue;
            blockNxNList.set(i, embedMessage(blockNxNList.get(i), k, mapIndexC, i));
            k++;
            positionKey += i+" ";
        }
        System.out.println(positionKey);
        if(positionKey.trim().split(" ").length != binaryArr.length){
            throw new Exception("Không đủ vị trí để nhúng");

        }
    }
    
    public void inverstDCT() {
        // Inverst DCT
        for (int i = 0; i < blockNxNList.size(); i++) {
            ArrayList<int[][]> blockLC = blockNxNList.get(i);
            blockLC.replaceAll(inputData -> dct.inverseDCT(dct.dequantitizeImage(inputData, true)));
            blockNxNList.set(i, blockLC);
        }
    }
    
    public void calBlockNxNlist() {
    	// Divide frame to 8x8 block pixel
        ArrayList<BufferedImage> block8x8List = CommonUtils.getBlock8x8Pixel(frame);
        
        // DCT each block pixel
        ArrayList<int[][]> blockDCT = new ArrayList<>();
        for (BufferedImage bufferedImage : block8x8List) {

            YCrCb[][] yCCmatrix = new YCrCb[8][8];
            int[][] matrix = new int[8][8];
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    Color color = new Color(bufferedImage.getRGB(x, y));
                    int red = color.getRed();
                    int green = color.getGreen();
                    int blue = color.getBlue();
                    YCrCb yCrCb = new YCrCb(red, green, blue);
                    yCCmatrix[x][y] = yCrCb;
                    matrix[x][y] = yCrCb.getY();
                }
            }
            int[][] matrixTest = dct.quantitizeImage(dct.forwardDCT(matrix), true);
            blockYCrCb.add(yCCmatrix);
            blockDCT.add(matrixTest);
        }
        
        // Relocate all block to ArrayList
        blockNxNList = getBlockListNxN(blockDCT);
    }

    /**
     * Get block list nxn from list block dct
     * @param blockDCT list block dct
     * @return block list nxn
     */
    private ArrayList<ArrayList<int[][]>> getBlockListNxN(ArrayList<int[][]> blockDCT) {
        ArrayList<ArrayList<int[][]>> res = CommonUtils.getBlockListNxN(
                blockDCT,
                frame.getWidth(),
                frame.getHeight(),
                n
        );
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
            for (int i = 64 - 1; i >= cMin; i--) {
                row = zigZag[i][0];
                col = zigZag[i][1];

                sumA += Math.abs(blockDCTA[row][col]) * Math.abs(blockDCTA[row][col]);
                sumB += Math.abs(blockDCTB[row][col]) * Math.abs(blockDCTB[row][col]);

                if (sumA >= d && sumB >= d) {
                    res = Math.max(i, res);
                }
            }
            if(res == -1) return null;
            mapIndexOffC.put(index, res);
        }
        
        return mapIndexOffC;
    }
	
	private ArrayList<int[][]> embedMessage(ArrayList<int[][]> blockLC, int i, Map<Integer, Integer> mapIndexC, int indexBlock) {
        int label = binaryArr[i];
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
    
    public ArrayList<YCrCb[][]> getBlockYCrCb() {
		return blockYCrCb;
	}
	
    public String getPositionKey() {
    	return positionKey;
    }
	
}
