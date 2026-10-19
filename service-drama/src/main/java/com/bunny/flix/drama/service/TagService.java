package com.bunny.flix.drama.service;

import com.bunny.flix.drama.entity.Tag;

import java.util.List;

public interface TagService {

    List<Tag> listTags(Long parentId);

    List<Tag> listAllTags();

    Tag getTag(Long id);

    Tag createTag(Tag tag);

    Tag updateTag(Tag tag);

    void deleteTag(Long id);
}
