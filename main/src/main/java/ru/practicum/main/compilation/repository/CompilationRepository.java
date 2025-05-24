package ru.practicum.main.compilation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.compilation.model.Compilation;

import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> findByPinned(Boolean pinned, Pageable pageable);

    Optional<Compilation> findByTitle(String title);
}
