package com.scholar.platform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class OpenAlexJsonl{

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    private static final Set<String> visitedAuthors = new HashSet<>();
    private static Map<String, Map<String, Integer>> coAuthorNetwork = new HashMap<>();
    private static Map<String, String> authorNames = new HashMap<>();
    private static String currentAuthor = null; // 当前处理的作者

    private static final File networkFile = new File("co_author_network.json");
    private static final File outputFile = new File("authors_with_network.jsonl");

    public static void main(String[] args) throws Exception {
        String startAuthor = "A5100450462"; // 起始作者 ID

        // 尝试加载已有的合作网络
        if (networkFile.exists()) {
            coAuthorNetwork = mapper.readValue(networkFile, new TypeReference<Map<String, Map<String, Integer>>>() {});
            visitedAuthors.addAll(coAuthorNetwork.keySet());
        }

        // 添加 Shutdown Hook，当程序被中断时保存当前状态
        Runtime.getRuntime().addShutdownHook(new Thread(OpenAlexJsonl::saveNetworkOnShutdown));

        // 广度优先队列
        Queue<String> queue = new LinkedList<>();
        queue.add(startAuthor);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
            while (!queue.isEmpty()) {
                String authorId = queue.poll();
                if (visitedAuthors.contains(authorId)) continue;

                currentAuthor = authorId;
                visitedAuthors.add(authorId);

                String url = "https://api.openalex.org/works?filter=author.id:" + authorId;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = mapper.readTree(response.body());
                JsonNode results = root.get("results");
                if (results == null || !results.isArray()) continue;

                for (JsonNode paper : results) {
                    JsonNode authorships = paper.get("authorships");
                    if (authorships != null && authorships.isArray()) {
                        List<String> authorIdsInPaper = new ArrayList<>();

                        for (JsonNode authorNode : authorships) {
                            JsonNode author = authorNode.get("author");
                            if (author != null) {
                                String id = author.get("id").asText(null);
                                String displayName = author.get("display_name").asText(null);
                                if (id != null) {
                                    authorIdsInPaper.add(id);
                                    authorNames.putIfAbsent(id, displayName);

                                    // 写入 JSONL
                                    ObjectNode out = mapper.createObjectNode();
                                    out.put("id", id);
                                    out.put("display_name", displayName);
                                    writer.write(mapper.writeValueAsString(out));
                                    writer.newLine();
                                }
                            }
                        }

                        // 更新合作网络
                        for (int i = 0; i < authorIdsInPaper.size(); i++) {
                            for (int j = i + 1; j < authorIdsInPaper.size(); j++) {
                                String a1 = authorIdsInPaper.get(i);
                                String a2 = authorIdsInPaper.get(j);

                                coAuthorNetwork.putIfAbsent(a1, new HashMap<>());
                                coAuthorNetwork.putIfAbsent(a2, new HashMap<>());

                                coAuthorNetwork.get(a1).put(a2, coAuthorNetwork.get(a1).getOrDefault(a2, 0) + 1);
                                coAuthorNetwork.get(a2).put(a1, coAuthorNetwork.get(a2).getOrDefault(a1, 0) + 1);
                            }
                        }

                        // 将未访问的作者加入队列
                        for (String nextAuthorId : authorIdsInPaper) {
                            if (!visitedAuthors.contains(nextAuthorId)) {
                                queue.add(nextAuthorId);
                            }
                        }
                    }
                }
            }
        }

        saveNetwork();
        System.out.println("处理完成，合作网络已保存。");
    }

    private static void saveNetworkOnShutdown() {
        try {
            System.out.println("\n程序被中断！");
            System.out.println("当前处理的作者: " + currentAuthor);
            System.out.println("已访问作者数量: " + visitedAuthors.size());
            saveNetwork();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveNetwork() throws IOException {
        // 保存 JSON 网络
        mapper.writerWithDefaultPrettyPrinter().writeValue(networkFile, coAuthorNetwork);

        // 保存 JSONL 文件，每个作者及其合作关系
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (String authorId : coAuthorNetwork.keySet()) {
                ObjectNode node = mapper.createObjectNode();
                node.put("id", authorId);
                node.put("display_name", authorNames.getOrDefault(authorId, ""));

                ArrayNode coauthorsArray = mapper.createArrayNode();
                for (Map.Entry<String, Integer> entry : coAuthorNetwork.get(authorId).entrySet()) {
                    ObjectNode coauthorNode = mapper.createObjectNode();
                    coauthorNode.put("id", entry.getKey());
                    coauthorNode.put("display_name", authorNames.getOrDefault(entry.getKey(), ""));
                    coauthorNode.put("count", entry.getValue());
                    coauthorsArray.add(coauthorNode);
                }
                node.set("coauthors", coauthorsArray);
                writer.write(mapper.writeValueAsString(node));
                writer.newLine();
            }
        }
    }
}
