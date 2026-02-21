
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TestAttachment {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting TestAttachment...");

        // 1. Create a dummy source file simulating what WhatsappService does
        Path uploadDir = Paths.get("uploads");
        if (!Files.exists(uploadDir))
            Files.createDirectories(uploadDir);

        String fileName = "WA_TEST_" + System.currentTimeMillis() + ".jpg";
        Path sourcePath = uploadDir.resolve(fileName);
        Files.write(sourcePath, "DUMMY IMAGE CONTENT".getBytes());
        System.out.println("Created source: " + sourcePath.toAbsolutePath());

        // 2. Simulate the string passed to ComplaintService
        String urlString = "/uploads/" + fileName;
        System.out.println("Input URL String: " + urlString);

        // 3. Replicate ComplaintService logic exactly
        String complaintId = "999"; // Dummy
        if (urlString.startsWith("/uploads/")) {
            String sourcePathStr = urlString.startsWith("/") ? urlString.substring(1) : urlString;
            Path src = Paths.get(sourcePathStr);
            System.out.println("Resolved Source Path: " + src.toAbsolutePath()); // Check resolution

            if (!Files.exists(src)) {
                System.out.println("ERROR: File not found at resolved path!");
            } else {
                System.out.println("File FOUND.");

                String destDirStr = "uploads/complaints/" + complaintId + "/";
                File dir = new File(destDirStr);
                if (!dir.exists())
                    dir.mkdirs();

                String destFilePath = destDirStr + fileName;
                Files.copy(src, Paths.get(destFilePath), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("SUCCESS: Copied to " + Paths.get(destFilePath).toAbsolutePath());
            }
        } else {
            System.out.println("Logic Skipped: Does not start with /uploads/");
        }
    }
}
