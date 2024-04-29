package com.api.comic_reader.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.api.comic_reader.entities.ChapterEntity;
import com.api.comic_reader.entities.ComicEntity;

@Repository
public interface ChapterRepository extends JpaRepository<ChapterEntity, Long> {
    @SuppressWarnings("null")
    Optional<ChapterEntity> findById(Long id);

    List<ChapterEntity> findByComic(ComicEntity comic);

    Optional<ChapterEntity> findTopByComicOrderByCreatedAtDesc(ComicEntity comic);
}
