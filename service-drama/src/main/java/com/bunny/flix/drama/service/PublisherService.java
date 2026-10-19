package com.bunny.flix.drama.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.drama.entity.Publisher;

public interface PublisherService {

    Page<Publisher> listPublishers(Integer page, Integer pageSize, String keyword);

    Publisher getPublisher(Long id);

    Publisher createPublisher(Publisher publisher);

    Publisher updatePublisher(Publisher publisher);

    void deletePublisher(Long id);
}
