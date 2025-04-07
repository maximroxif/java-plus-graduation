package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.HitDto;
import ru.practicum.HitStatDto;
import ru.practicum.stats.entity.Hit;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.stats.mapper.HitDtoMapper.dtoToHit;
import static ru.practicum.stats.mapper.HitDtoMapper.toHitDto;
import static ru.practicum.stats.utils.Constants.formatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public HitDto saveHit(HitDto hitDto) {
        log.info("Saving hit: {}", hitDto);
        Hit savedHit = statsRepository.save(dtoToHit(hitDto));
        log.debug("Successfully saved hit: {}", savedHit);
        return toHitDto(savedHit);
    }

    @Override
    public List<HitStatDto> getHits(String start, String end, List<String> uris, Boolean unique) {
        log.info("Retrieving stats: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        List<Hit> data;
        if (start == null || start.isBlank() || end == null || end.isBlank()) {
            log.warn("Start or end date is blank, fetching all data for uris: {}", uris);
            data = uris == null || uris.isEmpty() ? statsRepository.findAll() : statsRepository.findAllByUriIn(uris);
        } else {
            try {
                LocalDateTime startTime = LocalDateTime.parse(start, formatter);
                LocalDateTime endTime = LocalDateTime.parse(end, formatter);

                if (!startTime.isBefore(endTime)) {
                    log.error("Invalid date range: start={} is not before end={}", start, end);
                    throw new IllegalArgumentException("Start date must be before end date");
                }

                data = (uris == null || uris.isEmpty())
                        ? statsRepository.getStat(startTime, endTime)
                        : statsRepository.getStatByUris(startTime, endTime, uris);
                log.debug("Fetched {} hits from repository", data.size());
            } catch (Exception e) {
                log.error("Failed to parse dates or fetch stats: {}", e.getMessage(), e);
                throw new IllegalArgumentException("Invalid date format or retrieval error: " + e.getMessage(), e);
            }
        }

        Map<String, Map<String, List<Hit>>> groupedHits = data.stream()
                .collect(Collectors.groupingBy(
                        Hit::getApp,
                        Collectors.groupingBy(Hit::getUri)
                ));

        List<HitStatDto> result = new ArrayList<>();
        groupedHits.forEach((app, uriMap) -> uriMap.forEach((uri, hits) -> {
            HitStatDto stat = new HitStatDto();
            stat.setApp(app);
            stat.setUri(uri);
            List<String> ips = hits.stream().map(Hit::getIp).toList();
            int hitCount = unique ? (int) ips.stream().distinct().count() : ips.size();
            stat.setHits(hitCount);
            result.add(stat);
        }));

        List<HitStatDto> sortedResult = result.stream()
                .sorted(Comparator.comparingInt(HitStatDto::getHits).reversed())
                .toList();
        log.debug("Returning {} sorted stats", sortedResult.size());
        return sortedResult;
    }
}