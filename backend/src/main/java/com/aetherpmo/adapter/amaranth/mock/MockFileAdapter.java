package com.aetherpmo.adapter.amaranth.mock;

import com.aetherpmo.adapter.amaranth.FilePort;
import com.aetherpmo.adapter.amaranth.dto.FileMeta;
import com.aetherpmo.adapter.amaranth.dto.FileRef;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockFileAdapter implements FilePort {

    @Override
    public FileRef upload(String fileName, byte[] content) {
        String id = UUID.randomUUID().toString();
        return new FileRef(id, "https://mock.amaranth.local/files/" + id);
    }

    @Override
    public FileMeta getMeta(String fileId) {
        return new FileMeta(fileId, "mock-file.txt", "application/octet-stream", 0L);
    }

    @Override
    public byte[] download(String fileId) {
        return new byte[0];
    }
}
