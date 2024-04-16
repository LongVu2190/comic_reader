package com.api.comic_reader.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comic_user")
public class ComicUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private Date dateOfBirth;

    @Column(name = "is_male", nullable = false)
    private Boolean isMale;

    @Column(name = "is_banned", nullable = false, columnDefinition = "boolean default false")
    private Boolean isBanned;

    @JsonManagedReference
    @ManyToMany(fetch = FetchType.EAGER)
    private Collection<RoleEntity> roles;

    @JsonManagedReference
    @OneToMany(mappedBy = "comicUser", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RatingEntity> ratings;

    @JsonManagedReference
    @OneToMany(mappedBy = "comicUser", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<BookmarkEntity> bookmarks;
}
