package com.lostfound.web;

import com.lostfound.model.*;
import com.lostfound.service.ItemService;
import com.lostfound.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Read-only public API used by the web page (and handy for demos with curl/Postman). */
@RestController
@RequestMapping("/api")
public class ApiController {

    public record MatchDto(ItemDto item, int score, List<String> reasons) { }

    private final ItemService items;

    public ApiController(ItemService items) {
        this.items = items;
    }

    @GetMapping("/items")
    public List<ItemDto> search(@RequestParam(required = false) String q,
                                @RequestParam(required = false) ItemType type,
                                @RequestParam(required = false) Category category,
                                @RequestParam(required = false) ItemStatus status) {
        return items.search(q, type, category, status).stream().map(ItemDto::of).toList();
    }

    @GetMapping("/items/{id}")
    public ItemDto one(@PathVariable Long id) {
        try {
            return ItemDto.of(items.get(id));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/items/{id}/matches")
    public List<MatchDto> matches(@PathVariable Long id) {
        try {
            return items.matchesFor(id).stream()
                    .map((MatchingService.Match m) -> new MatchDto(ItemDto.of(m.item()), m.score(), m.reasons()))
                    .toList();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        return items.stats();
    }
}
