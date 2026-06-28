package com.aetherpmo.adapter.amaranth;

import com.aetherpmo.adapter.amaranth.dto.FileMeta;
import com.aetherpmo.adapter.amaranth.dto.FileRef;

public interface FilePort {

    FileRef upload(String fileName, byte[] content);

    FileMeta getMeta(String fileId);

    byte[] download(String fileId);
}
