import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class FileSimilarity {
    static Map<String, List<Long>> fileFingerprints = new HashMap<>();

    static Semaphore mutex = new Semaphore(1);

    static class FingerprintThread implements Runnable {
        String path;

        public FingerprintThread(String path){
            this.path = path;
        }

        @Override
        public void run() {
            try {
                List<Long> fingerprint = fileSum(path);
                mutex.acquire();
                fileFingerprints.put(path, fingerprint);
                mutex.release();
            } catch (Exception e) {System.out.println(e);}
        }
    }

    static class SimilarityThread implements Runnable {
        String file1;
        String file2;

        public SimilarityThread(String file1, String file2){
            this.file1 = file1;
            this.file2 = file2;
        }

        @Override
        public void run() {
            try {
                List<Long> fingerprint1 = fileFingerprints.get(file1);
                List<Long> fingerprint2 = fileFingerprints.get(file2);
                float similarityScore = similarity(fingerprint1, fingerprint2);
                System.out.println("Similarity between " + file1 + " and " + file2 + ": " + (similarityScore * 100) + "%");
            } catch (Exception e) {System.out.println(e);}
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Sum filepath1 filepath2 filepathN");
            System.exit(1);
        }

        List<Thread> thrList = new ArrayList<>();
        for (String path : args) {
            Thread thr1 = new Thread(new FingerprintThread(path));
            thrList.add(thr1);
            thr1.start();
        }

        for (Thread i : thrList) {
            i.join();
        }

        for (int i = 0; i < args.length; i++) {
            for (int j = i + 1; j < args.length; j++) {
                String file1 = args[i];
                String file2 = args[j];
                Thread thr2 = new Thread(new SimilarityThread(file1, file2));
                thr2.start();
            }
        }
    }

    private static List<Long> fileSum(String filePath) throws IOException {
        File file = new File(filePath);
        List<Long> chunks = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[100];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                long sum = sum(buffer, bytesRead);
                chunks.add(sum);
            }
        }
        return chunks;
    }

    private static long sum(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Byte.toUnsignedInt(buffer[i]);
        }
        return sum;
    }

    private static float similarity(List<Long> base, List<Long> target) {
        int counter = 0;
        Map<Long, Integer> freqs = new HashMap<Long, Integer>();
        for (Long e: target){
            if (freqs.get(e) == null){
                freqs.put(e, 1);
            }
            else{
                freqs.put(e, freqs.get(e)+1);
            }
        }
        for (Long value : base) {
            if (freqs.get(value) != null) {
                if (freqs.get(value) > 0){
                    counter++;
                    freqs.put(value, freqs.get(value)-1);
                }
            }
        }

        return (float) counter / base.size();
    }
    
}
