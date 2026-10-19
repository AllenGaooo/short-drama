package com.bunny.flix.drama.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.drama.entity.Drama;

import java.util.List;

public interface DramaService {

    Page<Drama> listDramas(Integer page, Integer pageSize, String keyword, Integer status, Integer payType, Long tagId);

    /**
     * 热门榜单
     * @param size   返回条数
     * @param sortBy 排序：hot(播放量) / newest(最新)
     */
    List<Drama> listRanking(Integer size, String sortBy);

    Drama getDramaById(Long id);

    Drama createDrama(Drama drama);

    Drama updateDrama(Drama drama);

    void deleteDrama(Long id);

    void updateStatus(Long id, Integer status);

    void updateCompleted(Long id, Integer completed);
}
