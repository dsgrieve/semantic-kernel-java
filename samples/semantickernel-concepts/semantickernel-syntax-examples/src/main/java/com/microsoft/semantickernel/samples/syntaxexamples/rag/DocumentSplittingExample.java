// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.samples.syntaxexamples.rag;

import com.microsoft.semantic.kernel.rag.splitting.Chunk;
import com.microsoft.semantic.kernel.rag.splitting.Document;
import com.microsoft.semantic.kernel.rag.splitting.Splitter;
import com.microsoft.semantic.kernel.rag.splitting.TextSplitter;
import com.microsoft.semantic.kernel.rag.splitting.document.TextDocument;
import com.microsoft.semantic.kernel.rag.splitting.overlap.NoOverlapCondition;
import com.microsoft.semantic.kernel.rag.splitting.splitconditions.CountSplitCondition;
import com.microsoft.semantic.kernel.rag.splitting.splitconditions.SplitPoint;
import com.microsoft.semantickernel.implementation.EmbeddedResourceLoader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DocumentSplittingExample {

    private static String BENEFITS_DOC = "https://raw.githubusercontent.com/Azure-Samples/azure-search-openai-demo-java/refs/heads/main/data/Benefit_Options.pdf";

    private static class PDFDocument implements Document {

        private final byte[] pdf;

        private PDFDocument(byte[] pdf) {
            this.pdf = pdf;
        }

        @Override
        public Flux<String> getContent() {
            try {
                PDFParser parser = new PDFParser(
                    RandomAccessReadBuffer.createBufferFromStream(new ByteArrayInputStream(pdf)));
                PDDocument document = parser.parse();
                String text = new PDFTextStripper().getText(document);

                return Flux.just(text);
            } catch (IOException e) {
                return Flux.error(e);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        useCustomChunker();
        useInbuiltChunker();
    }

    private static void useInbuiltChunker() throws IOException, InterruptedException {
        byte[] pdfBytes = getPdfDoc();
        PDFDocument pdfDoc = new PDFDocument(pdfBytes);

        Splitter splitter = Splitter
            .builder()
            .maxParagraphsPerChunk(4)
            .overlapNPercent(30.0f)
            .trimWhitespace()
            .build();

        List<Chunk> chunks = splitter
            .splitDocument(pdfDoc)
            .collectList()
            .block();

        chunks
            .forEach(chunk -> {
                System.out.println("=========");
                System.out.println(chunk.getContents());
            });
    }

    public static void useCustomChunker() throws IOException, InterruptedException {

        String example = EmbeddedResourceLoader.readFile("example.md",
            DocumentSplittingExample.class);

        // Define how we are splitting tokens, in this case we are splitting on headers of an md file
        // i.e <new line> followed by one or more # characters
        TextSplitter textSplitter = (doc, numTokens) -> {
            // Split on headers
            Pattern pattern = Pattern.compile("(\\r?\\n|\\r)\s*#+", Pattern.MULTILINE);

            Flux<Integer> splitPoints = Flux.fromStream(pattern.matcher(doc).results())
                .map(window -> window.start());

            return createWindows(doc, splitPoints);
        };

        // Split into single sections
        CountSplitCondition condition = new CountSplitCondition(1, textSplitter);

        Splitter splitter = Splitter
            .builder()
            .addChunkEndCondition(condition)
            // No overlap
            .setOverlapCondition(NoOverlapCondition.build())
            // Tidy up the text
            .trimWhitespace()
            .build();

        String chunks = splitter
            .splitDocument(new TextDocument(example))
            .collectList()
            .map(it -> it.stream()
                .map(chunk -> chunk.getContents())
                .collect(Collectors.joining("\n============\n")))
            .block();

        System.out.println(chunks);
    }

    /*
     * Transforms: [ 2, 10, 20, 100 ] -> [ (0, 2), (2, 10), (10, 20), (20, 100), (100, <doc length>)
     * ]
     */
    private static List<SplitPoint> createWindows(String doc, Flux<Integer> splitPoints) {
        return Flux.concat(
            Flux.just(0),
            splitPoints,
            Flux.just(doc.length()))
            .window(2, 1)
            .concatMap(window -> {
                return window.collectList()
                    .flatMap(list -> {
                        if (list.size() <= 1) {
                            return Mono.empty();
                        }
                        return Mono.just(
                            new SplitPoint(list.get(0), list.get(1)));
                    });
            })
            .collectList()
            .block();
    }

    private static byte[] getPdfDoc() throws IOException, InterruptedException {
        HttpResponse<byte[]> doc = HttpClient.newHttpClient()
            .send(HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BENEFITS_DOC))
                .build(),
                BodyHandlers.ofByteArray());
        return doc.body();
    }

}
