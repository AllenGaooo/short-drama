package com.bunny.flix.drama.service;

import com.bunny.flix.drama.entity.Episode;

import java.util.List;

public interface EpisodeService {

    List<Episode> listEpisodes(Long dramaId);

    Episode getEpisode(Long id);

    Episode createEpisode(Episode episode);

    Episode updateEpisode(Episode episode);

    void deleteEpisode(Long id);

    void updateSort(Long episodeId, Integer sortOrder);

    void updateFreeStatus(Long episodeId, Integer free);
}
