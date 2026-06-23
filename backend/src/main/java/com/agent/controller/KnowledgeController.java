package com.agent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeController {

    static class KnowledgeBase {
        public Long id;
        public String name;
        public String content;
        public String description;
        public int segmentCount;
        KnowledgeBase(Long id, String name, String content) {
            this.id = id;
            this.name = name;
            this.content = content;
            this.description = (content != null && content.length() > 100) ? content.substring(0, 100) + "..." : content;
            this.segmentCount = content == null ? 0 : content.split("\n").length;
        }
    }

    private final Map<Long, KnowledgeBase> store = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @GetMapping
    public List<KnowledgeBase> list() {
        return new ArrayList<>(store.values());
    }

    @PostMapping
    public KnowledgeBase create(@RequestBody Map<String, String> body) {
        KnowledgeBase kb = new KnowledgeBase(idGen.getAndIncrement(), body.get("name"), body.get("content"));
        store.put(kb.id, kb);
        return kb;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        store.remove(id);
        return Map.of("status", "ok");
    }
}
