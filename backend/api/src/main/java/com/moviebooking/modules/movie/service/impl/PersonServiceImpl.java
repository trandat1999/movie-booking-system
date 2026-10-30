package com.moviebooking.modules.movie.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.movie.dto.request.PersonRequest;
import com.moviebooking.modules.movie.dto.response.PersonResponse;
import com.moviebooking.modules.movie.entity.Person;
import com.moviebooking.modules.movie.mapper.MovieMapper;
import com.moviebooking.modules.movie.repository.PersonRepository;
import com.moviebooking.modules.movie.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PersonRepository personRepository;
    private final MovieMapper movieMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PersonResponse> search(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("fullName").and(Sort.by("id")));
        String term = keyword == null ? "" : keyword.trim();
        return PageResponse.from(
                personRepository.findByFullNameContainingIgnoreCase(term, pageable).map(movieMapper::toPerson));
    }

    @Override
    @Transactional
    public PersonResponse create(PersonRequest request) {
        Person person = new Person();
        apply(person, request);
        return movieMapper.toPerson(personRepository.save(person));
    }

    @Override
    @Transactional
    public PersonResponse update(Long id, PersonRequest request) {
        Person person = find(id);
        apply(person, request);
        return movieMapper.toPerson(person);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Person person = find(id);
        try {
            personRepository.delete(person);
            personRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.PERSON_IN_USE);
        }
    }

    private static void apply(Person person, PersonRequest request) {
        person.setFullName(request.fullName().trim());
        person.setAvatarUrl(request.avatarUrl() == null || request.avatarUrl().isBlank() ? null : request.avatarUrl().trim());
    }

    private Person find(Long id) {
        return personRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PERSON_NOT_FOUND));
    }
}
