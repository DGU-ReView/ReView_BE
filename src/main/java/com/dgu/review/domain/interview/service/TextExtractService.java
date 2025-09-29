package com.dgu.review.domain.interview.service;

import org.springframework.stereotype.Service;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;

import java.io.InputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.sax.BodyContentHandler;

@Service
public class TextExtractService {
    private final AutoDetectParser parser = new AutoDetectParser();
    
    // 예외 구체화하기 
    public String extractText(InputStream in) throws Exception {
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        try (InputStream is = in) {
            parser.parse(is, handler, metadata, context);
        }
        String text = handler.toString();
        return handler.toString();
    }
}