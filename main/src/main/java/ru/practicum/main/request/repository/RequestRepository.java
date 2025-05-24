package ru.practicum.main.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByRequesterId(Long requesterId);

    Optional<Request> findByIdAndRequesterId(Long requestId, Long requesterId);

    List<Request> findByEventIdAndEventInitiatorId(Long eventId, Long initiatorId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<Request> findAllByIdIn(List<Long> requestIds);

    @Query("SELECT r FROM Request r WHERE r.event.id = :eventId AND r.requester.id = :requesterId")
    Optional<Request> findByEventIdAndRequesterId(@Param("eventId") Long eventId,
                                                  @Param("requesterId") Long requesterId);

    @Query("SELECT r.event.id, COUNT(r.id) FROM Request r WHERE r.event.id IN :eventIds " +
            "AND r.status = 'CONFIRMED' GROUP BY r.event.id")
    List<Object[]> countConfirmedRequestsForEvents(@Param("eventIds") List<Long> eventIds);
}
