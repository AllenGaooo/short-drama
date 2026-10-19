package com.bunny.flix.search.service;

import com.bunny.flix.search.entity.HotWord;

import java.util.List;

public interface HotWordService {

    /**
     * 获取启用的热搜词列表
     */
    List<HotWord> listHotWords();

    /**
     * 新增热搜词
     */
    void addHotWord(HotWord hotWord);

    /**
     * 更新热搜词
     */
    void updateHotWord(HotWord hotWord);

    /**
     * 删除热搜词
     */
    void deleteHotWord(Long id);
}
