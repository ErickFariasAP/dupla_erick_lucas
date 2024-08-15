import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class FileSimilarity {
    static Map<String, List<Long>> fileFingerprints = new HashMap<>();

    static Semaphore mutex = new Semaphore(1);

    static class FingerprintThread extends Thread {
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

    static class SimilarityThread extends Thread {
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
            FingerprintThread thr = new FingerprintThread(path);
            thrList.add(thr);
            thr.start();
        }

        for (Thread i : thrList) {
            i.join();
        }

        // Compare each pair of files
        for (int i = 0; i < args.length; i++) {
            for (int j = i + 1; j < args.length; j++) {
                String file1 = args[i];
                String file2 = args[j];
                SimilarityThread thr = new SimilarityThread(file1, file2);
                thr.start();
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
        List<Long> targetCopy = new ArrayList<>(target);

        for (Long value : base) {
            if (targetCopy.contains(value)) {
                counter++;
                targetCopy.remove(value);
            }
        }

        return (float) counter / base.size();
    }
    
}
