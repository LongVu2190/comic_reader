package com.api.comic_reader.services;

import com.api.comic_reader.config.EnvironmentVariable;
import com.api.comic_reader.dtos.requests.ComicRequest;
import com.api.comic_reader.dtos.responses.ChapterResponse;
import com.api.comic_reader.dtos.responses.ComicResponse;
import com.api.comic_reader.entities.ComicEntity;
import com.api.comic_reader.exception.AppException;
import com.api.comic_reader.exception.ErrorCode;

import org.springframework.util.StringUtils;
import com.api.comic_reader.repositories.ComicRepository;

import lombok.AllArgsConstructor;

import java.util.stream.Collectors;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class ComicService {
    @Autowired
    private ComicRepository comicRepository;
    @Autowired
    private ChapterService chapterService;

    public List<ComicResponse> getAllComics() {
        List<ComicEntity> comics = comicRepository.findAll();

        if (comics.isEmpty()) {
            return Collections.emptyList();
        }

        return comics.stream().map(comic -> {
            String thumbnailUrl = EnvironmentVariable.baseUrl + "/api/comic/thumbnail/" + comic.getId();
            ChapterResponse lastestChapter = chapterService.getLastestChapter(comic.getId());

            return ComicResponse.builder()
                    .id(comic.getId())
                    .name(comic.getName())
                    .author(comic.getAuthor())
                    .description(comic.getDescription())
                    .thumbnailUrl(thumbnailUrl)
                    .view(comic.getView())
                    .lastestChapter(lastestChapter)
                    .isDeleted(comic.getIsDeleted())
                    .isFinished(comic.getIsFinished())
                    .build();
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ComicEntity insertComic(ComicRequest newComic) throws AppException {
        try {
            String originalFilename = newComic.getThumbnailImage().getOriginalFilename();
            if (originalFilename == null) {
                throw new AppException(ErrorCode.THUMBNAIL_INVALID);
            }
            String fileName = StringUtils.cleanPath(originalFilename);
            if (fileName.contains("..")) {
                throw new AppException(ErrorCode.THUMBNAIL_INVALID);
            }

            ComicEntity comic = ComicEntity.builder()
                    .name(newComic.getName())
                    .author(newComic.getAuthor())
                    .view(0L)
                    .description(newComic.getDescription())
                    .isFinished(false)
                    .isDeleted(false)
                    .thumbnailImage(newComic.getThumbnailImage().getBytes())
                    .build();

            return comicRepository.save(comic);

        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public byte[] getThumbnailImage(Long comicId) {
        Optional<ComicEntity> comicOptional = comicRepository.findById(comicId);
        if (comicOptional.isEmpty()) {
            throw new AppException(ErrorCode.COMIC_NOT_FOUND);
        }
        ComicEntity comic = comicOptional.get();
        return comic.getThumbnailImage();
    }
}
