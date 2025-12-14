package ru.practicum.ewm.compilation.service;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto createCompilation(NewCompilationDto dto);

    void deleteCompilation(Long id);

    CompilationDto updateCompilation(Long id, @Valid UpdateCompilationRequest dto);

    List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable);

    CompilationDto getCompilationById(Long compId);
}
