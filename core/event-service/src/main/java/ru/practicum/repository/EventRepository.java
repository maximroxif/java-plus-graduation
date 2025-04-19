package ru.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findAllByInitiatorId(long initiatorId, Pageable pageable);

    Optional<Event> findByInitiatorIdAndId(long initiatorId, long eventId);

    List<Event> findAllByIdIn(List<Long> eventId);

}
