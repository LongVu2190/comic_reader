package com.api.comic_reader.controllers;

import com.api.comic_reader.dtos.requests.ComicRequest;
import com.api.comic_reader.dtos.responses.ApiResponse;
import com.api.comic_reader.dtos.responses.ComicResponse;
import com.api.comic_reader.services.ComicService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/comic")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ComicController {

    @Autowired
    private final ComicService comicService;

    @GetMapping("/getAllComics")
    public ResponseEntity<ApiResponse> getAllComics() {
        List<ComicResponse> comics = comicService.getAllComics();

        return ResponseEntity.ok().body(
                ApiResponse
                        .builder()
                        .message("Get all comics successfully")
                        .result(comics)
                        .build());
    }

    @GetMapping("/get6LastestComics")
    public ResponseEntity<ApiResponse> get6LastestComics() {
        List<ComicResponse> comics = comicService.getAllComics();
    
        List<ComicResponse> _6LastestComics = comics.stream()
                .filter(comic -> comic.getLastestChapter() != null)
                .sorted(Comparator.comparing(
                        comic -> comic.getLastestChapter().getDateCreated(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .collect(Collectors.toList());
    
        return ResponseEntity.ok().body(
                ApiResponse
                        .builder()
                        .message("Get 10 lastest comics successfully")
                        .result(_6LastestComics)
                        .build());
    }

    @PostMapping("/insertComic")
    public ResponseEntity<ApiResponse> insertComic(
            @RequestPart("name") String name,
            @RequestPart("author") String author,
            @RequestPart("description") String description,
            @RequestPart("imageData") MultipartFile imageData) throws Exception {

        ComicRequest newComic = new ComicRequest();
        newComic.setName(name);
        newComic.setAuthor(author);
        newComic.setDescription(description);
        newComic.setThumbnailImage(imageData);

        comicService.insertComic(newComic);

        return ResponseEntity.ok().body(
                ApiResponse
                        .builder()
                        .message("Insert comic successfully")
                        .result(null)
                        .build());
    }

    @GetMapping("/thumbnail/{comicId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long comicId) throws Exception {
        byte[] thumbnail = comicService.getThumbnailImage(comicId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(thumbnail);
    }
}
