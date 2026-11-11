package com.reqflow.service.support;

import com.reqflow.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("doc", "docx", "xls", "xlsx", "pdf", "png", "jpg", "zip");

    private final Path root;

    public FileStorageService(@Value("${reqflow.storage.path:./data/uploads}") String path) throws IOException {
        this.root = Paths.get(path).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            extension = originalName.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED.contains(extension)) {
            throw new BizException("仅支持 doc/docx/xls/xlsx/pdf/png/jpg/zip 文件");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            file.transferTo(root.resolve(storedName));
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }
        return new StoredFile(originalName, storedName, file.getSize(), file.getContentType());
    }

    public Path load(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw new BizException("文件不存在");
        }
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new BizException("文件不存在");
        }
        return path;
    }

    public record StoredFile(String originalName, String storedName, long size, String contentType) {
    }
}
