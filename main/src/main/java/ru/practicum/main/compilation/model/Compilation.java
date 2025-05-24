package ru.practicum.main.compilation.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.main.event.model.Event;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "compilations")
public class Compilation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Boolean pinned;

    @Column(nullable = false, unique = true)
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "compilation_events", joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    private Set<Event> events = new HashSet<>();
}
