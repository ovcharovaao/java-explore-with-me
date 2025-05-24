package ru.practicum.main.request.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.user.model.User;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "requests", uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "requester_id"})})
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;
}
