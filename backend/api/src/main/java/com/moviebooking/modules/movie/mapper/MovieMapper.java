package com.moviebooking.modules.movie.mapper;

import com.moviebooking.modules.movie.dto.response.GenreResponse;
import com.moviebooking.modules.movie.dto.response.MovieDetailResponse;
import com.moviebooking.modules.movie.dto.response.MovieSummaryResponse;
import com.moviebooking.modules.movie.dto.response.PersonResponse;
import com.moviebooking.modules.movie.entity.Genre;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.entity.Person;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MovieMapper {

    GenreResponse toGenre(Genre genre);

    PersonResponse toPerson(Person person);

    List<PersonResponse> toPeople(List<Person> people);

    MovieSummaryResponse toSummary(Movie movie);

    @Mapping(
            target = "directors",
            expression = "java(toPeople(movie.peopleWithRole(com.moviebooking.modules.movie.enums.PersonRole.DIRECTOR)))")
    @Mapping(
            target = "actors",
            expression = "java(toPeople(movie.peopleWithRole(com.moviebooking.modules.movie.enums.PersonRole.ACTOR)))")
    MovieDetailResponse toDetail(Movie movie);

    default List<GenreResponse> toGenres(Set<Genre> genres) {
        return genres.stream()
                .sorted(Comparator.comparing(Genre::getName))
                .map(this::toGenre)
                .toList();
    }
}
